package no.nav.omsorgsdageraleneomsorgapi.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import java.util.*

private const val ID_PREFIX = "omd-alene-api-"

class KafkaAivenConfig (
    val properties: Properties
) {

    internal fun producer(name: String) = properties.apply {
        put(ProducerConfig.CLIENT_ID_CONFIG, "$ID_PREFIX$name")
    }

}