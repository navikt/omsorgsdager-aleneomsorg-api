package no.nav.omsorgsdageraleneomsorgapi

import no.nav.k9.søknad.JsonUtils
import no.nav.omsorgsdageraleneomsorgapi.k9format.tilK9Format
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class K9FormatTest {

    @Test
    fun `Søknad blir til korrekt k9Format`() {
        val søknadId = UUID.randomUUID().toString()
        val mottatt = ZonedDateTime.of(2021, 2, 1, 3, 4, 5, 6, ZoneId.of("UTC"))
        val k9Format = SøknadUtils.gyldigSøknad().copy(
            søknadId = søknadId,
            mottatt = mottatt
        ).tilK9Format(SøknadUtils.søker)

        //language=json
        val forventetK9FormatJson = """
            {
              "søknadId" : "$søknadId",
              "versjon" : "1.0.0",
              "mottattDato" : "2021-02-01T03:04:05.000Z",
              "søker" : {
                "norskIdentitetsnummer" : "02119970078"
              },
              "ytelse" : {
                "type" : "OMP_UTV_AO",
                "barn" : {
                  "norskIdentitetsnummer" : "25058118020",
                  "fødselsdato" : null
                },
                "periode" : "2021-01-01/..",
                "begrunnelse" : ""
              },
              "språk" : "nb",
              "journalposter" : [ ]
            }
        """.trimIndent()

        JSONAssert.assertEquals(forventetK9FormatJson, JsonUtils.toString(k9Format), true)
    }
}