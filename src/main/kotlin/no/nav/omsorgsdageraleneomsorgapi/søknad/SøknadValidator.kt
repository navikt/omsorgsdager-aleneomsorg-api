package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.helse.dusseldorf.ktor.core.ValidationProblemDetails
import no.nav.helse.dusseldorf.ktor.core.Violation
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.k9.søknad.Søknad as K9Søknad

internal fun Søknad.valider() {
    val mangler: MutableSet<Violation> = mutableSetOf()

    if (harBekreftetOpplysninger er false) {
        mangler.add(
            Violation(
                parameterName = "harBekreftetOpplysninger",
                parameterType = ParameterType.ENTITY,
                reason = "Opplysningene må bekreftes for å sende inn søknad.",
                invalidValue = harBekreftetOpplysninger
            )
        )
    }

    if (harForståttRettigheterOgPlikter er false) {
        mangler.add(
            Violation(
                parameterName = "harForståttRettigheterOgPlikter",
                parameterType = ParameterType.ENTITY,
                reason = "Må ha forstått rettigheter og plikter for å sende inn søknad.",
                invalidValue = harForståttRettigheterOgPlikter
            )
        )
    }

    if(barn.isEmpty()){
        mangler.add(
            Violation(
                parameterName = "barn",
                parameterType = ParameterType.ENTITY,
                reason = "Listen over barn kan ikke være tom",
                invalidValue = barn
            )
        )
    }

    barn.mapIndexed { index, barnSøknad -> mangler.addAll(barnSøknad.valider(index)) }

    if (mangler.isNotEmpty()) {
        throw Throwblem(ValidationProblemDetails(mangler))
    }
}

internal infix fun Boolean?.er(forventetVerdi: Boolean?): Boolean = this == forventetVerdi

fun K9Søknad.valider() {
    val mangler = OmsorgspengerAleneOmsorg.MinValidator().valider(getYtelse<OmsorgspengerAleneOmsorg>()).map {
        Violation(
            parameterName = it.felt,
            parameterType = ParameterType.ENTITY,
            reason = it.feilmelding,
            invalidValue = "k9-format feilkode: ${it.feilkode}"
        )
    }.toSet()

    if (mangler.isNotEmpty()) {
        throw Throwblem(ValidationProblemDetails(mangler))
    }
}