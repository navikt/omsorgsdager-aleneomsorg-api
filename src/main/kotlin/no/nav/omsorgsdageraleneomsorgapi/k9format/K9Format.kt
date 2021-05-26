package no.nav.omsorgsdageraleneomsorgapi.k9format

import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.Søknad
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
            Periode(mottatt.toLocalDate(), null),
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