package no.nav.omsorgsdageraleneomsorgapi.søknad

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.unleash.UnleashService
import no.nav.omsorgsdageraleneomsorgapi.barn.BarnService
import no.nav.omsorgsdageraleneomsorgapi.felles.SØKNAD_URL
import no.nav.omsorgsdageraleneomsorgapi.felles.UnleashFeatures
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
import java.time.ZoneOffset
import java.time.ZonedDateTime

private val logger: Logger = LoggerFactory.getLogger("nav.soknadApis")

fun Route.søknadApis(
    søknadService: SøknadService,
    barnService: BarnService,
    søkerService: SøkerService,
    idTokenProvider: IdTokenProvider,
    unleashService: UnleashService
) {

    post(SØKNAD_URL) {
        logger.info("Mottatt ny søknad")
        val søknad = call.receive<Søknad>()

        val barnMedIdentitetsnummer = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        søknad.oppdaterBarnMedIdentitetsnummer(barnMedIdentitetsnummer)
        logger.info("Oppdatering av identitetsnummer på barn OK")

        val idToken = idTokenProvider.getIdToken(call)
        val callId = call.getCallId()
        val mottatt = ZonedDateTime.now(ZoneOffset.UTC)
        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)

        søker.validate()
        søknad.valider()

        logger.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        val skalLeggePåKø = unleashService.isEnabled(UnleashFeatures.SKAL_LEGGE_PÅ_KØ, true)
        if(skalLeggePåKø) {
            søknadService.leggPåKø(
                søknad = søknad,
                metadata = call.metadata(),
                mottatt = mottatt,
                søker = søker
            )
        } else logger.info("Søknad ikke lagt på kø pga Unleashfeature ")

        call.respond(HttpStatusCode.Accepted)
    }

    post(VALIDERING_URL) {
        val søknad = call.receive<Søknad>()
        val idToken = idTokenProvider.getIdToken(call)
        val callId = call.getCallId()
        val mottatt = ZonedDateTime.now(ZoneOffset.UTC)

        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)

        val barnMedIdentitetsnummer = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        søknad.oppdaterBarnMedIdentitetsnummer(barnMedIdentitetsnummer)
        logger.info("Oppdatering av identitetsnummer på barn OK")

        søknad.valider()
        call.respond(HttpStatusCode.Accepted)
    }
}