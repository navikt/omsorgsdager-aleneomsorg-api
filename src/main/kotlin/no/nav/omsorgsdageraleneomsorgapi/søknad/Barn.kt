package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import no.nav.omsorgsdageraleneomsorgapi.felles.gyldigNorskIdentifikator

data class Barn (
    val navn: String,
    val aktørId: String?,
    var identitetsnummer: String?,
    val aleneomsorg: Boolean? = null
) {
    fun manglerIdentitetsnummer(): Boolean = identitetsnummer.isNullOrEmpty()

    infix fun oppdaterIdentitetsnummerMed(identitetsnummer: String?){
        this.identitetsnummer = identitetsnummer
    }

    fun valider(index: Int): MutableSet<Violation> {
        val mangler: MutableSet<Violation> = mutableSetOf()

        if(aleneomsorg er null){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].aleneomsorg",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn.aleneomsorg kan ikke være null",
                    invalidValue = aleneomsorg
                )
            )
        }

        if(identitetsnummer == null){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].identitetsnummer",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn.identitetsnummer kan ikke være null",
                    invalidValue = identitetsnummer
                )
            )
        }

        if(identitetsnummer != null && !identitetsnummer!!.gyldigNorskIdentifikator()){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].identitetsnummer",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn.identitetsnummer må være gyldig norsk identifikator",
                    invalidValue = identitetsnummer
                )
            )
        }

        if(navn.isNullOrBlank()){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].navn",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn.navn må kan ikke være null, tom eller bare mellomrom",
                    invalidValue = navn
                )
            )
        }

        return mangler
    }
}