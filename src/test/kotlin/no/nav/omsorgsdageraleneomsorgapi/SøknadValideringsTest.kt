package no.nav.omsorgsdageraleneomsorgapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdageraleneomsorgapi.felles.starterMedFodselsdato
import no.nav.omsorgsdageraleneomsorgapi.søknad.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.søknad.valider
import org.junit.Test
import java.time.ZonedDateTime
import kotlin.test.assertTrue

internal class SøknadValideringsTest {

    companion object {
        private val ugyldigFødselsnummer = "12345678900"
        private val mottatt = ZonedDateTime.now()
    }

    @Test
    fun `Tester gyldig fødselsdato dersom dnunmer`() {
        val starterMedFodselsdato = "630293".starterMedFodselsdato()
        assertTrue(starterMedFodselsdato)
    }

    @Test
    fun `Gyldig søknad`() {
        val søknad = SøknadUtils.gyldigSøknad
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            harForståttRettigheterOgPlikter = false
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harBekreftetOpplysninger er false`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            harBekreftetOpplysninger = false
        )
        søknad.valider()
    }

    @Test(expected =  Throwblem::class)
    fun `Feiler dersom barn ikke har identitetsnummer`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            barn = listOf(
                Barn(
                    navn = "Ole Dole Doffen",
                    aktørId = null,
                    identitetsnummer = null
                )
            )
        )
        søknad.valider()
    }

    @Test(expected =  Throwblem::class)
    fun `Feiler dersom barn ikke har gyldig identitetsnummer`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            barn = listOf(
                Barn(
                    navn = "Ole Dole Doffen",
                    aktørId = null,
                    identitetsnummer = "ikke gyldig"
                )
            )
        )
        søknad.valider()
    }

    @Test(expected =  Throwblem::class)
    fun `Feiler dersom barn ikke har navn`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            barn = listOf(
                Barn(
                    navn = "",
                    aktørId = "12345",
                    identitetsnummer = "12345"
                )
            )
        )
        søknad.valider()
    }

    @Test(expected =  Throwblem::class)
    fun `Feiler dersom barn er tom liste`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            barn = listOf()
        )
        søknad.valider()
    }

}
