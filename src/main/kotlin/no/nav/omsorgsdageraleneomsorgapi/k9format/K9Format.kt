package no.nav.omsorgsdageraleneomsorgapi.k9format

import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.Søknad
import no.nav.omsorgsdageraleneomsorgapi.søknad.TidspunktForAleneomsorg
import java.time.LocalDate
import no.nav.k9.søknad.Søknad as K9Søknad
import no.nav.k9.søknad.felles.personopplysninger.Barn as K9Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker as K9Søker

fun Søknad.tilK9Format(søker: Søker) : K9Søknad {
    return K9Søknad(
        SøknadId( søknadId),
        Versjon.of("1.0.0"),
        mottatt,
        søker.tilK9Søker(),
        OmsorgspengerAleneOmsorg(
            this.barn.tilK9Barn(),
            Periode(this.tilPeriodeFraOgMed(), null),
            ""
        )
    )
}

private fun Søker.tilK9Søker() : K9Søker = K9Søker(NorskIdentitetsnummer.of(fødselsnummer))

private fun List<Barn>.tilK9Barn() : K9Barn? {
    val barn = this.getOrNull(0)
    return if(barn != null) K9Barn(NorskIdentitetsnummer.of(barn.identitetsnummer))
    else null
}

private fun Søknad.tilPeriodeFraOgMed() : LocalDate {
    return when(this.barn[0].tidspunktForAleneomsorg){
        TidspunktForAleneomsorg.SISTE_2_ÅRENE -> this.barn[0].dato!!
        TidspunktForAleneomsorg.TIDLIGERE -> LocalDate.parse("${forrigeÅr()}-01-01")
    }
}

private fun forrigeÅr() = LocalDate.now().year.minus(1)