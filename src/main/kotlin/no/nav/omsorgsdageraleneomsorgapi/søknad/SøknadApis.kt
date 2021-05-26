package no.nav.omsorgsdageraleneomsorgapi.søknad

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdageraleneomsorgapi.barn.BarnService
import no.nav.omsorgsdageraleneomsorgapi.felles.SØKNAD_URL
import no.nav.omsorgsdageraleneomsorgapi.felles.VALIDERING_URL
import no.nav.omsorgsdageraleneomsorgapi.felles.formaterStatuslogging
import no.nav.omsorgsdageraleneomsorgapi.general.CallId
import no.nav.omsorgsdageraleneomsorgapi.general.auth.IdToken
import no.nav.omsorgsdageraleneomsorgapi.general.auth.IdTokenProvider
import no.nav.omsorgsdageraleneomsorgapi.general.getCallId
import no.nav.omsorgsdageraleneomsorgapi.general.metadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOGGER: Logger = LoggerFactory.getLogger("nav.soknadApis")

fun Route.søknadApis(
    søknadService: SøknadService,
    barnService: BarnService,
    idTokenProvider: IdTokenProvider
) {

    post(SØKNAD_URL) {
        val søknad = call.receive<Søknad>()
        LOGGER.info(formaterStatuslogging(søknad.søknadId, "mottatt"))

        val(idToken, callId) = call.hentIdTokenOgCallId(idTokenProvider)

        søknadService.registrer(
            søknad = søknad,
            metadata = call.metadata(),
            idToken = idToken,
            callId = callId
        )

        call.respond(HttpStatusCode.Accepted)
    }

    post(VALIDERING_URL) {
        val søknad = call.receive<Søknad>()
        LOGGER.info(formaterStatuslogging(søknad.søknadId, "valideres"))

        val(idToken, callId) = call.hentIdTokenOgCallId(idTokenProvider)
        val barnMedIdentitetsnummer = barnService.hentNåværendeBarn(idToken, callId)
        søknad.oppdaterBarnMedIdentitetsnummer(barnMedIdentitetsnummer)

        søknad.valider()
        LOGGER.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        call.respond(HttpStatusCode.Accepted)
    }
}

private fun ApplicationCall.hentIdTokenOgCallId(idTokenProvider: IdTokenProvider): Pair<IdToken, CallId> =
    Pair(idTokenProvider.getIdToken(this), getCallId())