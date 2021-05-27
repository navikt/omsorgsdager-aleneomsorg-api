package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.omsorgsdageraleneomsorgapi.barn.BarnOppslag
import no.nav.omsorgsdageraleneomsorgapi.k9format.tilK9Format
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

data class Søknad(
    val søknadId: String = UUID.randomUUID().toString(),
    val mottatt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    val språk: String,
    val barn: List<Barn> = listOf(),
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
) {

    fun tilKomplettSøknad(søker: Søker): KomplettSøknad = KomplettSøknad(
        mottatt = mottatt,
        søker = søker,
        søknadId = søknadId,
        språk = språk,
        barn = this.barn[0], //Etter splitt er det kun et barn i lista.
        k9Søknad = this.tilK9Format(søker),
        harBekreftetOpplysninger = harBekreftetOpplysninger,
        harForståttRettigheterOgPlikter = harForståttRettigheterOgPlikter
    )

    fun oppdaterBarnMedIdentitetsnummer(listeOverBarnOppslag: List<BarnOppslag>) {
        barn.forEach { barn ->
            if (barn.manglerIdentitetsnummer()) {
                barn oppdaterIdentitetsnummerMed listeOverBarnOppslag.hentIdentitetsnummerForBarn(barn.aktørId)
            }
        }
    }
}

private fun List<BarnOppslag>.hentIdentitetsnummerForBarn(aktørId: String?) = find { it.aktørId == aktørId }?.identitetsnummer