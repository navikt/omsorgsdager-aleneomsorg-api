package no.nav.omsorgsdageraleneomsorgapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdageraleneomsorgapi.felles.starterMedFodselsdato
import no.nav.omsorgsdageraleneomsorgapi.søknad.valider
import org.junit.jupiter.api.Assertions
import kotlin.test.Test
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

    @Test
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`(){
        Assertions.assertThrows(Throwblem::class.java){
            SøknadUtils.gyldigSøknad().copy(
                harForståttRettigheterOgPlikter = false
            ).valider()
        }
    }

    @Test
    fun `Feiler dersom harBekreftetOpplysninger er false`(){
        Assertions.assertThrows(Throwblem::class.java){
            SøknadUtils.gyldigSøknad().copy(
                harBekreftetOpplysninger = false
            ).valider()
        }
    }

    @Test
    fun `Feiler dersom barn er tom liste`(){
        Assertions.assertThrows(Throwblem::class.java){
            SøknadUtils.gyldigSøknad().copy(
                barn = listOf()
            ).valider()
        }
    }

}