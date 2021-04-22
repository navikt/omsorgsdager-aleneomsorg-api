package no.nav.omsorgsdageraleneomsorgapi.søknad.søknad

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation

enum class Situasjon {
    @JsonAlias("innlagtIHelseinstitusjon")
    INNLAGT_I_HELSEINSTITUSJON,

    @JsonAlias("utøverVerneplikt")
    UTØVER_VERNEPLIKT,

    @JsonAlias("fengsel")
    FENGSEL,

    @JsonAlias("sykdom")
    SYKDOM,

    @JsonAlias("annet")
    ANNET
}

internal fun AnnenForelder.validerSituasjon(): MutableSet<Violation> = mutableSetOf<Violation>().apply {
    addAll(validerFellesFelter())

    when (situasjon) {
        Situasjon.INNLAGT_I_HELSEINSTITUSJON -> addAll(validerInnlagtIHelseInstitusjon(situasjon))
        Situasjon.UTØVER_VERNEPLIKT, Situasjon.FENGSEL -> addAll(validerVærnepliktEllerFengsel(situasjon))
        Situasjon.SYKDOM, Situasjon.ANNET -> addAll(validerSykdomEllerAnnet(situasjon))
    }
}

private fun AnnenForelder.validerSykdomEllerAnnet(situasjon: Situasjon) = mutableSetOf<Violation>().apply {
    addAll(validerBekreftelseOmPeriodeOver6Mnd(situasjon))
    addAll(validerSituasjonBeskrivelse(situasjon))
}

private fun AnnenForelder.validerInnlagtIHelseInstitusjon(s: Situasjon) = mutableSetOf<Violation>().apply {
    addAll(validerBekreftelseOmPeriodeOver6Mnd(s))
}

private fun AnnenForelder.validerVærnepliktEllerFengsel(situasjon: Situasjon) = mutableSetOf<Violation>().apply {
    if (periodeTilOgMed == null) {
        add(
            Violation(
                parameterName = "AnnenForelder.periodeTilOgMed",
                parameterType = ParameterType.ENTITY,
                reason = "periodeTilOgMed kan ikke være null dersom situasjonen er $situasjon",
                invalidValue = periodeTilOgMed
            )
        )
    }
}

private fun AnnenForelder.validerBekreftelseOmPeriodeOver6Mnd(situasjon: Situasjon) = mutableSetOf<Violation>().apply {
    if (periodeTilOgMed == null && periodeOver6Måneder == null) {
        add(
            Violation(
                parameterName = "AnnenForelder.periodeOver6Måneder",
                parameterType = ParameterType.ENTITY,
                reason = "periodeOver6Måneder kan ikke være null når periodeTilOgMed er null, og situasjonen er $situasjon",
                invalidValue = periodeFraOgMed
            )
        )
    }
}

private fun AnnenForelder.validerFellesFelter() = mutableSetOf<Violation>().apply {
    if (periodeTilOgMed != null && periodeFraOgMed.isAfter(periodeTilOgMed)) {
        add(
            Violation(
                parameterName = "AnnenForelder.periodeFraOgMed",
                parameterType = ParameterType.ENTITY,
                reason = "periodeFraOgMed kan ikke være etter periodeTilOgMed",
                invalidValue = periodeFraOgMed
            )
        )
    }
}

private fun AnnenForelder.validerSituasjonBeskrivelse(situasjon: Situasjon) = mutableSetOf<Violation>().apply {
    if (situasjonBeskrivelse.isNullOrBlank()) {
        add(
            Violation(
                parameterName = "AnnenForelder.situasjonBeskrivelse",
                parameterType = ParameterType.ENTITY,
                reason = "Situasjonsbeskrivelse på annenForelder kan ikke være null, tom eller kun white spaces når situasjon er $situasjon",
                invalidValue = situasjonBeskrivelse
            )
        )
    }

    if (!situasjonBeskrivelse.isNullOrBlank() && (situasjonBeskrivelse.length !in 5..1000)) {
        add(
            Violation(
                parameterName = "AnnenForelder.situasjonBeskrivelse",
                parameterType = ParameterType.ENTITY,
                reason = "Situasjonsbeskrivelse på annenForelder kan kun ha en lengde mellom 5 til 1000 tegn.",
                invalidValue = situasjonBeskrivelse.length
            )
        )
    }
}
