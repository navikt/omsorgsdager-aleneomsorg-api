package no.nav.omsorgsdageraleneomsorgapi

import com.github.fppt.jedismock.RedisServer
import com.github.tomakehurst.wiremock.http.Cookie
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestUtils.getAuthCookie
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.omsorgsdageraleneomsorgapi.felles.*
import no.nav.omsorgsdageraleneomsorgapi.kafka.Topics
import no.nav.omsorgsdageraleneomsorgapi.mellomlagring.started
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.TidspunktForAleneomsorg
import no.nav.omsorgsdageraleneomsorgapi.wiremock.omsorgsdagerAleneomsorgApi
import no.nav.omsorgsdageraleneomsorgapi.wiremock.stubK9OppslagBarn
import no.nav.omsorgsdageraleneomsorgapi.wiremock.stubK9OppslagSoker
import no.nav.omsorgsdageraleneomsorgapi.wiremock.stubOppslagHealth
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val gyldigFodselsnummerA = "290990123456"
private const val gyldigFodselsnummerB = "25118921464"
private const val ikkeMyndigFnr = "12125012345"

class ApplicationTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

        val wireMockServer = WireMockBuilder()
            .withAzureSupport()
            .withNaisStsSupport()
            .withLoginServiceSupport()
            .omsorgsdagerAleneomsorgApi()
            .build()
            .stubOppslagHealth()
            .stubK9OppslagBarn()
            .stubK9OppslagSoker()

        val redisServer: RedisServer = RedisServer.newRedisServer().started()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()

        fun getConfig(kafkaEnvironment: KafkaEnvironment): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    kafkaEnvironment = kafkaEnvironment,
                    wireMockServer = wireMockServer,
                    redisServer = redisServer
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)

            return HoconApplicationConfig(mergedConfig)
        }


        val engine = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })


        @BeforeAll
        @JvmStatic
        fun buildUp() {
            engine.start(wait = true)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            redisServer.stop()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Hente søker`() {
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.OK,
            expectedResponse = """
                {
                  "aktørId": "12345",
                  "fødselsdato": "1997-05-25",
                  "fødselsnummer": "290990123456",
                  "fornavn": "MOR",
                  "mellomnavn": "HEISANN",
                  "etternavn": "MORSEN",
                  "myndig": true
                }
            """.trimIndent()
        )
    }

    @Test
    fun `Hente søker som ikke er myndig`() {
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.OK,
            cookie = getAuthCookie(ikkeMyndigFnr),
            expectedResponse = """
                {
                  "aktørId": "12345",
                  "fødselsdato": "2050-12-12",
                  "fødselsnummer": "12125012345",
                  "fornavn": "MOR",
                  "mellomnavn": "HEISANN",
                  "etternavn": "MORSEN",
                  "myndig": false
                }
            """.trimIndent()
        )
    }

    @Test
    fun `Hente søker hvor man får 451 fra oppslag`() {
        wireMockServer.stubK9OppslagSoker(
            statusCode = HttpStatusCode.fromValue(451),
            responseBody =
            //language=json
            """
            {
                "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
                "instance": "/meg",
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451
            }
            """.trimIndent()
        )

        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.fromValue(451),
            expectedResponse =
            //language=json
            """
            {
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451,
                "instance": "/soker",
                "detail": "Tilgang nektet."
            }
            """.trimIndent(),
            cookie = getAuthCookie(ikkeMyndigFnr)
        )

        wireMockServer.stubK9OppslagSoker() // reset til default mapping
    }

    @Test
    fun `Sende søknad hvor søker ikke er myndig`() {
        val cookie = getAuthCookie(ikkeMyndigFnr)

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = """
                {
                    "type": "/problem-details/unauthorized",
                    "title": "unauthorized",
                    "status": 403,
                    "detail": "Søkeren er ikke myndig og kan ikke sende inn søknaden.",
                    "instance": "about:blank"
                }
            """.trimIndent(),
            expectedCode = HttpStatusCode.Forbidden,
            cookie = cookie,
            requestEntity = SøknadUtils.gyldigSøknad().somJson()
        )
    }

    @Test
    fun `Hente barn og sjekk eksplisit at identitetsnummer ikke blir med ved get kall`() {

        val respons = requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = BARN_URL,
            expectedCode = HttpStatusCode.OK,
            //language=json
            expectedResponse = """
                {
                  "barnOppslag": [
                    {
                      "fødselsdato": "2000-08-27",
                      "fornavn": "BARN",
                      "mellomnavn": "EN",
                      "etternavn": "BARNESEN",
                      "aktørId": "1000000000001"
                    },
                    {
                      "fødselsdato": "2001-04-10",
                      "fornavn": "BARN",
                      "mellomnavn": "TO",
                      "etternavn": "BARNESEN",
                      "aktørId": "1000000000002"
                    }
                  ]
                }
            """.trimIndent()
        ).content

        val responsSomJSONArray = JSONObject(respons).getJSONArray("barnOppslag")

        assertFalse(responsSomJSONArray.getJSONObject(0).has("identitetsnummer"))
        assertFalse(responsSomJSONArray.getJSONObject(1).has("identitetsnummer"))
    }

    @Test
    fun `Feil ved henting av barn skal returnere tom liste`() {
        wireMockServer.stubK9OppslagBarn(simulerFeil = true)
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = BARN_URL,
            expectedCode = HttpStatusCode.OK,
            expectedResponse = """
            {
                "barnOppslag": []
            }
            """.trimIndent(),
            cookie = getAuthCookie(gyldigFodselsnummerB)
        )
        wireMockServer.stubK9OppslagBarn()
    }

    @Test
    fun `Hente barn hvor man får 451 fra oppslag`(){
        wireMockServer.stubK9OppslagBarn(
            statusCode = HttpStatusCode.fromValue(451),
            responseBody =
            //language=json
            """
            {
                "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
                "instance": "/meg",
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451
            }
            """.trimIndent()
        )

        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = BARN_URL,
            expectedCode = HttpStatusCode.fromValue(451),
            expectedResponse =
            //language=json
            """
            {
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451,
                "instance": "/barn",
                "detail": "Tilgang nektet."
            }
            """.trimIndent(),
            cookie = getAuthCookie(ikkeMyndigFnr)
        )

        wireMockServer.stubK9OppslagBarn() // reset til default mapping
    }

    @Test
    fun `Sende gyldig melding til validering`() {
        val søknad = SøknadUtils.gyldigSøknad().somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = VALIDERING_URL,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad
        )
    }

    @Test
    fun `Sende ugyldig søknad til validering`() {
        val ugyldigSøknad = SøknadUtils.gyldigSøknad().copy(
            barn = listOf(),
            harBekreftetOpplysninger = false,
            harForståttRettigheterOgPlikter = false
        ).somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = VALIDERING_URL,
            expectedResponse = """
                {
                  "type": "/problem-details/invalid-request-parameters",
                  "title": "invalid-request-parameters",
                  "status": 400,
                  "detail": "Requesten inneholder ugyldige paramtere.",
                  "instance": "about:blank",
                  "invalid_parameters": [
                    {
                      "type": "entity",
                      "name": "harBekreftetOpplysninger",
                      "reason": "Opplysningene må bekreftes for å sende inn søknad.",
                      "invalid_value": false
                    },
                    {
                      "type": "entity",
                      "name": "harForståttRettigheterOgPlikter",
                      "reason": "Må ha forstått rettigheter og plikter for å sende inn søknad.",
                      "invalid_value": false
                    },
                    {
                      "type": "entity",
                      "name": "barn",
                      "reason": "Listen over barn kan ikke være tom",
                      "invalid_value": []
                    }
                  ]
                }
            """.trimIndent(),
            expectedCode = HttpStatusCode.BadRequest,
            requestEntity = ugyldigSøknad
        )
    }

    @Test
    fun `Sende ugyldig søknad til innsending`() {
        val ugyldigSøknad = SøknadUtils.gyldigSøknad().copy(
            barn = listOf(),
            harBekreftetOpplysninger = false,
            harForståttRettigheterOgPlikter = false
        ).somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = """
                {
                  "type": "/problem-details/invalid-request-parameters",
                  "title": "invalid-request-parameters",
                  "status": 400,
                  "detail": "Requesten inneholder ugyldige paramtere.",
                  "instance": "about:blank",
                  "invalid_parameters": [
                    {
                      "type": "entity",
                      "name": "harBekreftetOpplysninger",
                      "reason": "Opplysningene må bekreftes for å sende inn søknad.",
                      "invalid_value": false
                    },
                    {
                      "type": "entity",
                      "name": "harForståttRettigheterOgPlikter",
                      "reason": "Må ha forstått rettigheter og plikter for å sende inn søknad.",
                      "invalid_value": false
                    },
                    {
                      "type": "entity",
                      "name": "barn",
                      "reason": "Listen over barn kan ikke være tom",
                      "invalid_value": []
                    }
                  ]
                }
            """.trimIndent(),
            expectedCode = HttpStatusCode.BadRequest,
            requestEntity = ugyldigSøknad
        )
    }

    @Test
    fun `Sende gyldig søknad og plukke opp fra kafka topic`() {
        val søknad = SøknadUtils.gyldigSøknad().somJson()

        val correlationId = requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad
        ).call.callId!!

        val søknadSendtTilProsessering = hentSøknadSendtTilProsessering(correlationId)
        verifiserAtInnholdetErLikt(JSONObject(søknad), søknadSendtTilProsessering)
    }

    @Test
    fun `Sende gyldig søknad med to barn, forvent at det blir plukket opp to meldinger fra topicen`() {
        val søknad = SøknadUtils.gyldigSøknad().copy(
            barn = listOf(
                Barn(
                    navn = "Ole",
                    identitetsnummer = "25058118020",
                    aktørId = "12345",
                    tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
                    dato = LocalDate.parse("2021-01-01")
                ),
                Barn(
                    navn = "Doffen",
                    identitetsnummer = "03127900263",
                    aktørId = "12345",
                    tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
                    dato = LocalDate.parse("2021-01-01")
                )
            )
        )

        val correlationId = requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad.somJson()
        ).call.callId!!

        logger.info("CorrelationID: $correlationId")
        val søknaderHentetFraProsessering = hentFlereSøknadSendtTilProsessering(correlationId, forventetAntall = 2)

        assertTrue(søknaderHentetFraProsessering.size == 2)

        val barnsIdentitetsnummer = søknaderHentetFraProsessering.map { it.getJSONObject("barn").getString("identitetsnummer") }
        assertTrue(barnsIdentitetsnummer.contains("25058118020"))
        assertTrue(barnsIdentitetsnummer.contains("03127900263"))
    }

    // TODO: 25/05/2021 Test som sjekker at dersom noe går galt ved kafkaprodusering så sendes ikke noe

    private fun requestAndAssert(
        httpMethod: HttpMethod,
        path: String,
        requestEntity: String? = null,
        expectedResponse: String?,
        expectedCode: HttpStatusCode,
        leggTilCookie: Boolean = true,
        cookie: Cookie = getAuthCookie(gyldigFodselsnummerA)
    ): TestApplicationResponse {
        val testApplicationResponse: TestApplicationResponse
        with(engine) {
            handleRequest(httpMethod, path) {
                if (leggTilCookie) addHeader(HttpHeaders.Cookie, cookie.toString())
                logger.info("Request Entity = $requestEntity")
                addHeader(HttpHeaders.Accept, "application/json")
                if (requestEntity != null) addHeader(HttpHeaders.ContentType, "application/json")
                if (requestEntity != null) setBody(requestEntity)
            }.apply {
                testApplicationResponse = response
                logger.info("Response Entity = ${response.content}")
                logger.info("Expected Entity = $expectedResponse")
                assertEquals(expectedCode, response.status())
                if (expectedResponse != null) {
                    JSONAssert.assertEquals(expectedResponse, response.content!!, true)
                } else {
                    assertEquals(expectedResponse, response.content)
                }
            }
        }
        return testApplicationResponse
    }

    private fun hentSøknadSendtTilProsessering(correlationId: String) = kafkaTestConsumer.hentSøknad(
        correlationId = correlationId,
        topic = Topics.MOTTATT_OMD_ALENEOMSORG
    ).data

    private fun hentFlereSøknadSendtTilProsessering(correlationId: String, forventetAntall: Int) = kafkaTestConsumer.hentFlereSøknader(
        correlationId = correlationId,
        topic = Topics.MOTTATT_OMD_ALENEOMSORG,
        forventetAntall = forventetAntall
    )


    private fun verifiserAtInnholdetErLikt(
        søknadSendtInn: JSONObject,
        søknadPlukketFraTopic: JSONObject
    ) {
        assertTrue(søknadPlukketFraTopic.has("søker"))
        søknadPlukketFraTopic.remove("søker") //Fjerner søker siden det settes i komplettSøknad

        assertTrue(søknadPlukketFraTopic.has("k9Søknad"))
        søknadPlukketFraTopic.remove("k9Søknad") //Fjerner k9Søknad siden det settes i komplettSøknad

        søknadPlukketFraTopic.remove("barn")
        søknadSendtInn.remove("barn")

        JSONAssert.assertEquals(søknadSendtInn, søknadPlukketFraTopic, true)

        logger.info("Verifisering OK")
    }
}