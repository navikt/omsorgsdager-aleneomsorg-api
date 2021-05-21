package no.nav.omsorgsdageraleneomsorgapi

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import no.nav.omsorgsdageraleneomsorgapi.søknad.Barn
import no.nav.omsorgsdageraleneomsorgapi.søknad.TidspunktForAleneomsorg
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class BarnValideringTest {

    val gyldigBarn = Barn(
        navn = "Jens",
        aktørId = "123456",
        identitetsnummer = "25058118020",
        tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
        dato = LocalDate.now().minusMonths(1)
    )

    @Test
    fun `Gyldig barn gir ingen feil`() {
        val feil = gyldigBarn.valider(0)
        assertEquals(0, feil.size)
    }

    @Test
    fun `Skal feile dersom identitetsnummer er null`() {
        val feil = gyldigBarn.copy(identitetsnummer = "").valider(0)

        assertEquals(1, feil.size)
        assertEquals(
            expected = Violation(
                parameterName = "barn[0].identitetsnummer",
                parameterType = ParameterType.ENTITY,
                reason = "Barn.identitetsnummer må være gyldig norsk identifikator",
                invalidValue = ""
            ),
            actual = feil.first()
        )
    }

    @Test
    fun `Skal feile dersom identitetsnummer ikke er gyldig`() {
        val feil = gyldigBarn.copy(identitetsnummer = "123").valider(0)

        assertEquals(1, feil.size)
        assertEquals(
            expected = Violation(
                parameterName = "barn[0].identitetsnummer",
                parameterType = ParameterType.ENTITY,
                reason = "Barn.identitetsnummer må være gyldig norsk identifikator",
                invalidValue = "123"
            ),
            actual = feil.first()
        )
    }

    @Test
    fun `Skal feile dersom navn er tomt`() {
        val feil = gyldigBarn.copy(navn = "").valider(0)

        assertEquals(1, feil.size)
        assertEquals(
            expected = Violation(
                parameterName = "barn[0].navn",
                parameterType = ParameterType.ENTITY,
                reason = "Barn.navn må kan ikke være null, tom eller bare mellomrom",
                invalidValue = ""
            ),
            actual = feil.first()
        )
    }

    @Test
    fun `Skal feile dersom navn er kun mellomrom`() {
        val feil = gyldigBarn.copy(navn = "   ").valider(0)

        assertEquals(1, feil.size)
        assertEquals(
            expected = Violation(
                parameterName = "barn[0].navn",
                parameterType = ParameterType.ENTITY,
                reason = "Barn.navn må kan ikke være null, tom eller bare mellomrom",
                invalidValue = "   "
            ),
            actual = feil.first()
        )
    }

    @Test
    fun `Skal feile dersom tidspunktForAleneomsorg er siste 2 år, men dato er ikke satt`() {
        val feil = gyldigBarn.copy(
            tidspunktForAleneomsorg = TidspunktForAleneomsorg.SISTE_2_ÅRENE,
            dato = null
        ).valider(0)

        assertEquals(1, feil.size)
        assertEquals(
            expected = Violation(
                parameterName = "barn[0].dato",
                parameterType = ParameterType.ENTITY,
                reason = "Barn.dato kan ikke være tom dersom tidspunktForAleneomsorg er SISTE_2_ÅRENE",
                invalidValue = null
            ),
            actual = feil.first()
        )
    }

}