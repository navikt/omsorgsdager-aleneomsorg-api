package no.nav.omsorgsdageraleneomsorgapi.barn

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdageraleneomsorgapi.felles.BARN_URL
import no.nav.omsorgsdageraleneomsorgapi.general.auth.IdTokenProvider
import no.nav.omsorgsdageraleneomsorgapi.general.getCallId

fun Route.barnApis(
    barnService: BarnService,
    idTokenProvider: IdTokenProvider
) {
    get(BARN_URL){
        val barn = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        call.respond(BarnResponse(barn))
    }
}
