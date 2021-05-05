package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.omsorgsdageraleneomsorgapi.felles.Metadata
import no.nav.omsorgsdageraleneomsorgapi.felles.formaterStatuslogging
import no.nav.omsorgsdageraleneomsorgapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(SøknadService::class.java)

class SøknadService(private val kafkaProducer: SøknadKafkaProducer) {

    fun leggPåKø(søknad: Søknad, metadata: Metadata, søker: Søker) {
        logger.info(formaterStatuslogging(søknad.søknadId, "registreres"))
        kafkaProducer.produserKafkamelding(søknad = søknad.tilKomplettSøknad(søker), metadata = metadata)
    }

}