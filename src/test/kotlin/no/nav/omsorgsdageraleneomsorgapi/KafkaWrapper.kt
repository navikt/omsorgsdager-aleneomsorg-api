package no.nav.omsorgsdageraleneomsorgapi

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.omsorgsdageraleneomsorgapi.felles.Metadata
import no.nav.omsorgsdageraleneomsorgapi.kafka.TopicEntry
import no.nav.omsorgsdageraleneomsorgapi.kafka.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.json.JSONObject
import java.time.Duration
import kotlin.test.assertEquals

private const val username = "srvkafkaclient"
private const val password = "kafkaclient"

object KafkaWrapper {
    fun bootstrap() : KafkaEnvironment {
        val kafkaEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = true,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames= listOf(
                Topics.MOTTATT_OMD_ALENEOMSORG
            )
        )
        return kafkaEnvironment
    }
}

private fun KafkaEnvironment.testConsumerProperties() : MutableMap<String, Any>?  {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        put(ConsumerConfig.GROUP_ID_CONFIG, "omsorgsdager-aleneomsorg-api")
    }
}

internal fun KafkaEnvironment.testConsumer() : KafkaConsumer<String, TopicEntry<JSONObject>> {
    val consumer = KafkaConsumer<String, TopicEntry<JSONObject>>(
        testConsumerProperties(),
        StringDeserializer(),
        SoknadV1OutgoingDeserialiser()
    )
    consumer.subscribe(listOf(Topics.MOTTATT_OMD_ALENEOMSORG))
    return consumer
}

internal fun KafkaConsumer<String, TopicEntry<JSONObject>>.hentS??knad(
    correlationId: String,
    maxWaitInSeconds: Long = 20,
    topic: String
) : TopicEntry<JSONObject> {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(topic)
            .filter { it.value().metadata.correlationId == correlationId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke opprettet oppgave for s??knad $correlationId etter $maxWaitInSeconds sekunder.")
}

internal fun KafkaConsumer<String, TopicEntry<JSONObject>>.hentFlereS??knader(
    correlationId: String,
    maxWaitInSeconds: Long = 20,
    topic: String,
    forventetAntall: Int
) : List<JSONObject> {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(topic)
            .filter { it.value().metadata.correlationId == correlationId }

        if (entries.isNotEmpty()) {
            assertEquals(forventetAntall, entries.size)
            return entries.map { it.value().data }
        }
    }
    throw IllegalStateException("Fant ikke opprettet oppgave for s??knad $correlationId etter $maxWaitInSeconds sekunder.")
}

private class SoknadV1OutgoingDeserialiser : Deserializer<TopicEntry<JSONObject>> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun deserialize(topic: String, data: ByteArray): TopicEntry<JSONObject> {
        val json = JSONObject(String(data))
        val metadata = json.getJSONObject("metadata")
        return TopicEntry(
            metadata = Metadata(
                version = metadata.getInt("version"),
                correlationId = metadata.getString("correlationId")
            ),
            data = json.getJSONObject("data")
        )
    }
    override fun close() {}

}
