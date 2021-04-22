package no.nav.omsorgsdageraleneomsorgapi.søknad.søknad

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import no.nav.omsorgsdageraleneomsorgapi.felles.gyldigNorskIdentifikator
import java.time.LocalDate

data class AnnenForelder(
    val navn: String,
    val fnr: String,
    val situasjon: Situasjon,
    val situasjonBeskrivelse: String? = null,
    val periodeOver6Måneder: Boolean? = null, //Settes til null for å unngå default false
    @JsonFormat(pattern = "yyyy-MM-dd") val periodeFraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val periodeTilOgMed: LocalDate? = null // TODO: 20/04/2021 Fjern nullable etter lansering. Felt skal alltid settes.
)

internal fun AnnenForelder.valider(): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()

    if(navn.isNullOrBlank()){
        mangler.add(
            Violation(
                parameterName = "AnnenForelder.navn",
                parameterType = ParameterType.ENTITY,
                reason = "Navn på annen forelder kan ikke være null, tom eller kun white spaces",
                invalidValue = navn
            )
        )
    }

    if(fnr.gyldigNorskIdentifikator() er false){
        mangler.add(
            Violation(
                parameterName = "AnnenForelder.fnr",
                parameterType = ParameterType.ENTITY,
                reason = "Fødselsnummer på annen forelder må være gyldig norsk identifikator",
                invalidValue = fnr
            )
        )
    }

    mangler.addAll(validerSituasjon())

    return mangler
}
