package no.nav.omsorgsdageraleneomsorgapi

import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.Søknad
import java.time.LocalDate

object SøknadUtils {

    val søker = Søker(
        aktørId = "12345",
        fødselsdato = LocalDate.parse("2000-01-01"),
        fødselsnummer = "02119970078",
        fornavn = "Ole",
        mellomnavn = "Dole",
        etternavn = "Doffen"
    )

    fun gyldigSøknad() = Søknad(
        id = "123456789",
        språk = "nb",
        barn = listOf(
            Barn(
                navn = "Ole Dole",
                identitetsnummer = "25058118020",
                aktørId = null,
                aleneomsorg = true
            )
        ),
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true
    )
}