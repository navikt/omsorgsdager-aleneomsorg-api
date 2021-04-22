package no.nav.omsorgsdageraleneomsorgapi.søknad.søknad

import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import java.time.ZonedDateTime

data class KomplettSøknad(
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val søknadId: String,
    val id: String,
    val språk: String,
    val annenForelder: AnnenForelder,
    val barn: List<Barn>,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
)