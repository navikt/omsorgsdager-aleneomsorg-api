package no.nav.omsorgsdageraleneomsorgapi.barn

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.auth.IdTokenProvider
import no.nav.omsorgsdageraleneomsorgapi.felles.BARN_URL
import no.nav.omsorgsdageraleneomsorgapi.general.getCallId
import no.nav.omsorgsdageraleneomsorgapi.general.oppslag.TilgangNektetException
import no.nav.omsorgsdageraleneomsorgapi.general.oppslag.respondTilgangNektetProblemDetail
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.omsorgsdageraleneomsorgapi.barn.barnApis")

fun Route.barnApis(
    barnService: BarnService,
    idTokenProvider: IdTokenProvider
) {
    get(BARN_URL) {
        try {
            val barn = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
            call.respond(BarnResponse(barn))
        } catch (e: Exception) {
            when (e) {
                is TilgangNektetException -> call.respondTilgangNektetProblemDetail(logger, e)
                else -> throw e
            }
        }
    }
}