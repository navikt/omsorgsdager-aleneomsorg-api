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
import no.nav.omsorgsdageraleneomsorgapi.general.auth.IdTokenProvider
import no.nav.omsorgsdageraleneomsorgapi.general.getCallId
import no.nav.omsorgsdageraleneomsorgapi.general.metadata
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søker.SøkerService
import no.nav.omsorgsdageraleneomsorgapi.søker.validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.soknadApis")

fun Route.søknadApis(
    søknadService: SøknadService,
    barnService: BarnService,
    søkerService: SøkerService,
    idTokenProvider: IdTokenProvider
) {

    post(SØKNAD_URL) {
        val søknad = call.receive<Søknad>()
        logger.info(formaterStatuslogging(søknad.søknadId, "mottatt"))

        val barnMedIdentitetsnummer = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        søknad.oppdaterBarnMedIdentitetsnummer(barnMedIdentitetsnummer)

        val idToken = idTokenProvider.getIdToken(call)
        val callId = call.getCallId()
        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)

        søker.validate()
        søknad.valider()
        logger.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        søknadService.leggPåKø(
            søknad = søknad,
            metadata = call.metadata(),
            søker = søker
        )

        call.respond(HttpStatusCode.Accepted)
    }

    post(VALIDERING_URL) {
        val søknad = call.receive<Søknad>()
        val idToken = idTokenProvider.getIdToken(call)
        val callId = call.getCallId()
        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)

        val barnMedIdentitetsnummer = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        søknad.oppdaterBarnMedIdentitetsnummer(barnMedIdentitetsnummer)
        logger.info("Oppdatering av identitetsnummer på barn OK")

        søknad.valider()
        call.respond(HttpStatusCode.Accepted)
    }
}