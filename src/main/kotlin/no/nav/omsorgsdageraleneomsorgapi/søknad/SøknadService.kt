package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.omsorgsdageraleneomsorgapi.felles.Metadata
import no.nav.omsorgsdageraleneomsorgapi.felles.formaterStatuslogging
import no.nav.omsorgsdageraleneomsorgapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søker.SøkerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime


class SøknadService(
    private val søkerService: SøkerService,
    private val kafkaProducer: SøknadKafkaProducer
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SøknadService::class.java)
    }

    fun leggPåKø(
        søknad: Søknad,
        metadata: Metadata,
        mottatt: ZonedDateTime,
        søker: Søker
    ) {
        logger.info(formaterStatuslogging(søknad.søknadId, "registreres"))

        val komplettSøknad = søknad.tilKomplettSøknad(søker, mottatt)
        kafkaProducer.produce(søknad = komplettSøknad, metadata = metadata)
    }
}