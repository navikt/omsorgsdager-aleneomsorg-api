package no.nav.omsorgsdageraleneomsorgapi.general

import io.ktor.application.*
import io.ktor.features.*
import no.nav.omsorgsdageraleneomsorgapi.felles.Metadata

data class CallId(val value : String)

fun ApplicationCall.getCallId() : CallId {
    return CallId(callId!!)
}

fun ApplicationCall.metadata() = Metadata(
    version = 1, //Versjonering ved store endringer.
    correlationId = getCallId().value
)