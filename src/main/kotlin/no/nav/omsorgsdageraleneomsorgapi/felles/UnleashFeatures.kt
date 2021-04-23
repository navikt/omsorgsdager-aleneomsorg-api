package no.nav.omsorgsdageraleneomsorgapi.felles

import no.nav.helse.dusseldorf.ktor.unleash.UnleashFeature

enum class UnleashFeatures : UnleashFeature {
    SKAL_LEGGE_PÅ_KØ(){
        override fun featureName() = "sif.omd.aleneomsorg.leggePaaKoe"
    }
}