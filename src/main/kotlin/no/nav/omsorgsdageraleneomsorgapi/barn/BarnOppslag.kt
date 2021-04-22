package no.nav.omsorgsdageraleneomsorgapi.barn

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

data class BarnResponse(
    val barnOppslag: List<BarnOppslag>
)

data class BarnOppslag (
    val fødselsdato: LocalDate,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val aktørId: String?,
    @JsonIgnore var identitetsnummer: String? = null
)