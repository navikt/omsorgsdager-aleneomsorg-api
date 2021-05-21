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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOGGER: Logger = LoggerFactory.getLogger(SøknadService::class.java)

class SøknadService(
    val kafkaProducer: SøknadKafkaProducer,
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
            val søknader = søknad.splittTilKomplettSøknadPerBarn(søker)
            LOGGER.info("SøknadId:${søknad.søknadId} splittet ut til ${søknader.map { it.søknadId }}")

            søknader.forEach {
                kafkaProducer.produserKafkamelding(søknad = it, metadata = metadata)
            }
        } else {
            kafkaProducer.produserKafkamelding(søknad = søknad.tilKomplettSøknad(søker), metadata = metadata)
        }
    }

}

fun Søknad.splittTilKomplettSøknadPerBarn(søker: Søker): List<KomplettSøknad> {
    return this.barn.mapIndexed { index, barn ->
        this.tilKomplettSøknad(søker).copy(barn = listOf(barn), søknadId = søknadId+"-${index+1}")
    // TODO: 21/05/2021 Er det best å legge på -X på original uuid, eller burde man generere helt ny uuid?
    //  Kan noe feile fordi den er 2 tegn lengre? Vi har uansett correlationId for å følge, samt logger hva de blir spilttet ut til.
    // Fordelen med å legge på postfix er at det er enklere å teste fordi da kjenner man til søknadId.
    }
}