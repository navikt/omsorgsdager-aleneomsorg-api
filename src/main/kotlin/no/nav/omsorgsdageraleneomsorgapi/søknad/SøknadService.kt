package no.nav.omsorgsdageraleneomsorgapi.søknad

import no.nav.omsorgsdageraleneomsorgapi.barn.BarnService
import no.nav.omsorgsdageraleneomsorgapi.felles.Metadata
import no.nav.omsorgsdageraleneomsorgapi.felles.formaterStatuslogging
import no.nav.omsorgsdageraleneomsorgapi.general.CallId
import no.nav.omsorgsdageraleneomsorgapi.general.auth.IdToken
import no.nav.omsorgsdageraleneomsorgapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdageraleneomsorgapi.søker.Søker
import no.nav.omsorgsdageraleneomsorgapi.søker.SøkerService
import no.nav.omsorgsdageraleneomsorgapi.søker.validate
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.common.errors.OutOfOrderSequenceException
import org.apache.kafka.common.errors.ProducerFencedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val LOGGER: Logger = LoggerFactory.getLogger(SøknadService::class.java)

class SøknadService(
    val kafkaProdusent: SøknadKafkaProducer,
    val barnService: BarnService,
    val søkerService: SøkerService
) {

    suspend fun registrer(søknad: Søknad, metadata: Metadata, idToken: IdToken, callId: CallId) {
        LOGGER.info(formaterStatuslogging(søknad.søknadId, "registreres"))

        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)
        søker.validate()

        val barnMedIdentitetsnummer = barnService.hentNåværendeBarn(idToken, callId)
        søknad.oppdaterBarnMedIdentitetsnummer(barnMedIdentitetsnummer)

        søknad.valider()
        LOGGER.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        if (søknad.barn.size > 1) {
            val søknader = søknad.splittTilSøknadPerBarn()
            LOGGER.info("SøknadId:${søknad.søknadId} splittet ut til ${søknader.map { it.søknadId }}")
            try {
                kafkaProdusent.beginTransaction()
                søknader.forEach { kafkaProdusent.produserKafkamelding(søknad = it.tilKomplettSøknad(søker), metadata = metadata) }
                kafkaProdusent.commitTransaction()
            } catch (e: Exception) {
                LOGGER.error("Feilet ved produsering av kafkamelding")
                when (e) {
                    is ProducerFencedException, is OutOfOrderSequenceException, is AuthorizationException -> {
                        // We can't recover from these exceptions, so our only option is to close the producer and exit.
                        kafkaProdusent.close()
                    }
                    else -> kafkaProdusent.abortTransaction()
                }
                throw e
            }
        } else {
            kafkaProdusent.beginTransaction()
            kafkaProdusent.produserKafkamelding(søknad = søknad.tilKomplettSøknad(søker), metadata = metadata)
            kafkaProdusent.commitTransaction()
        }
    }

}

fun Søknad.splittTilSøknadPerBarn() = this.barn.map { this.copy(barn = listOf(it), søknadId = UUID.randomUUID().toString()) }