package no.nav.omsorgsdageraleneomsorgapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdageraleneomsorgapi.felles.starterMedFodselsdato
import no.nav.omsorgsdageraleneomsorgapi.søknad.valider
import org.junit.Test
import kotlin.test.assertTrue

internal class SøknadValideringsTest {

    @Test
    fun `Tester gyldig fødselsdato dersom dnunmer`() {
        val starterMedFodselsdato = "630293".starterMedFodselsdato()
        assertTrue(starterMedFodselsdato)
    }

    @Test
    fun `Gyldig søknad`() {
        SøknadUtils.gyldigSøknad().valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`(){
        SøknadUtils.gyldigSøknad().copy(
            harForståttRettigheterOgPlikter = false
        ).valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harBekreftetOpplysninger er false`(){
        SøknadUtils.gyldigSøknad().copy(
            harBekreftetOpplysninger = false
        ).valider()
    }

    @Test(expected =  Throwblem::class)
    fun `Feiler dersom barn er tom liste`(){
        SøknadUtils.gyldigSøknad().copy(
            barn = listOf()
        ).valider()
    }

}