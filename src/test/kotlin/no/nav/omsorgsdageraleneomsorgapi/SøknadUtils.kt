package no.nav.omsorgsdageraleneomsorgapi

import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.Søknad
import no.nav.omsorgsdageraleneomsorgapi.søknad.TidspunktForAleneomsorg
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

object SøknadUtils {

    val søker = Søker(
        aktørId = "12345",
        fødselsdato = LocalDate.parse("2000-01-01"),
        fødselsnummer = "02119970078",
        fornavn = "Ole",
        mellomnavn = "Dole",
        etternavn = "Doffen"
    )

    fun gyldigSøknad(mottatt: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)) = Søknad(
        språk = "nb",
        barn = listOf(
            Barn(
                navn = "Ole Dole",
                identitetsnummer = "25058118020",
                aktørId = "12345",
                tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
                dato = LocalDate.parse("2021-01-01")
            )
        ),
        mottatt = mottatt,
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true
    )
}