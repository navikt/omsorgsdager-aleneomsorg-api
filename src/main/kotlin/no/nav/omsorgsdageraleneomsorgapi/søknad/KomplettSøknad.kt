package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.k9.søknad.Søknad
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import java.time.ZonedDateTime

data class KomplettSøknad(
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val søknadId: String,
    val språk: String,
    val barn: Barn,
    val k9Søknad: Søknad,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
)