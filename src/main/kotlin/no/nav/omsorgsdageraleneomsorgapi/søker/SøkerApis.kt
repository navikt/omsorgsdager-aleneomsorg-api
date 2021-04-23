package no.nav.omsorgsdageraleneomsorgapi.søker

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdageraleneomsorgapi.felles.SØKER_URL
import no.nav.omsorgsdageraleneomsorgapi.general.auth.IdTokenProvider
import no.nav.omsorgsdageraleneomsorgapi.general.getCallId

@KtorExperimentalLocationsAPI
fun Route.søkerApis(
    søkerService: SøkerService,
    idTokenProvider: IdTokenProvider
) {
    get(SØKER_URL) {
        val søker = søkerService.getSøker(idTokenProvider.getIdToken(call), call.getCallId())
        call.respond(søker)
    }
}

