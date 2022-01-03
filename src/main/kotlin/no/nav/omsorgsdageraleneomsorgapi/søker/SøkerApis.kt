package no.nav.omsorgsdageraleneomsorgapi.søker

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.auth.IdTokenProvider
import no.nav.omsorgsdageraleneomsorgapi.felles.SØKER_URL
import no.nav.omsorgsdageraleneomsorgapi.general.getCallId
import no.nav.omsorgsdageraleneomsorgapi.general.oppslag.TilgangNektetException
import no.nav.omsorgsdageraleneomsorgapi.general.oppslag.respondTilgangNektetProblemDetail
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.omsorgsdageraleneomsorgapi.søker.søkerApis")

fun Route.søkerApis(
    søkerService: SøkerService,
    idTokenProvider: IdTokenProvider
) {
    get(SØKER_URL) {
        try {
            val søker = søkerService.getSøker(idTokenProvider.getIdToken(call), call.getCallId())
            call.respond(søker)
        } catch (e: Exception) {
            when (e) {
                is TilgangNektetException -> call.respondTilgangNektetProblemDetail(logger, e)
                else -> throw e
            }
        }
    }
}

