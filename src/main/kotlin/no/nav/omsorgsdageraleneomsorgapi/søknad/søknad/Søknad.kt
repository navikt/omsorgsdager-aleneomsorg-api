package no.nav.omsorgsdageraleneomsorgapi.søknad.søknad

import no.nav.omsorgsdageraleneomsorgapi.barn.BarnOppslag
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import java.time.ZonedDateTime
import java.util.*

data class Søknad(
    val søknadId: String = UUID.randomUUID().toString(),
    val id: String,
    val språk: String,
    val barn: List<Barn> = listOf(),
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
) {

    fun tilKomplettSøknad(søker: Søker, mottatt: ZonedDateTime): KomplettSøknad = KomplettSøknad(
        mottatt = mottatt,
        søker = søker,
        søknadId = søknadId,
        id = id,
        språk = språk,
        barn = barn,
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

private fun List<BarnOppslag>.hentIdentitetsnummerForBarn(aktørId: String?): String? {
    return find {
        it.aktørId == aktørId
    }?.identitetsnummer
}