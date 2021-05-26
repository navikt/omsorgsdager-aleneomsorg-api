import no.nav.omsorgsdageraleneomsorgapi.SøknadUtils
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.TidspunktForAleneomsorg
import no.nav.omsorgsdageraleneomsorgapi.søknad.splittTilSøknadPerBarn
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SøknadSplittTest {

    @Test
    fun `Søknad bestående av tre barn blir splittet ut til tre egne søknader per barn`(){
        val barn = listOf(
            Barn(
                navn = "Ole",
                identitetsnummer = "25058118020",
                aktørId = null,
                tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
                dato = LocalDate.parse("2021-01-01")
            ),
            Barn(
                navn = "Dole",
                identitetsnummer = "19017822821",
                aktørId = null,
                tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
                dato = LocalDate.parse("2021-01-01")
            ),
            Barn(
                navn = "Doffen",
                identitetsnummer = "03127900263",
                aktørId = null,
                tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
                dato = LocalDate.parse("2021-01-01")
            )
        )

        val søknadSplitt = SøknadUtils.gyldigSøknad().copy(barn = barn).splittTilSøknadPerBarn()
        val barnFraSplitt = søknadSplitt.map { it.barn[0] }

        assertEquals(søknadSplitt.size, barn.size)
        assertEquals(barn, barnFraSplitt)
    }
}