package no.nav.omsorgsdageraleneomsorgapi.kafka

import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.omsorgsdageraleneomsorgapi.felles.Metadata
import no.nav.omsorgsdageraleneomsorgapi.felles.formaterStatuslogging
import no.nav.omsorgsdageraleneomsorgapi.felles.somJson
import no.nav.omsorgsdageraleneomsorgapi.søknad.KomplettSøknad
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.json.JSONObject
import org.slf4j.LoggerFactory

class SøknadKafkaProdusent(val kafkaConfig: KafkaConfig) : HealthCheck {
    private companion object {
        private val NAME = "SøknadProdusent"
        private val OMD_ALENEOMSORG_MOTTATT_TOPIC = TopicUse(
            name = Topics.MOTTATT_OMD_ALENEOMSORG,
            valueSerializer = SøknadSerializer()
        )

        private val logger = LoggerFactory.getLogger(SøknadKafkaProdusent::class.java)
    }

    private val produsent = KafkaProducer<String, TopicEntry<JSONObject>>(
        kafkaConfig.producer(NAME),
        OMD_ALENEOMSORG_MOTTATT_TOPIC.keySerializer(),
        OMD_ALENEOMSORG_MOTTATT_TOPIC.valueSerializer
    )

    init {
        produsent.initTransactions()
    }

    internal fun beginTransaction() = produsent.beginTransaction()
    internal fun abortTransaction() = produsent.abortTransaction()
    internal fun commitTransaction() = produsent.commitTransaction()
    internal fun close() = produsent.close()

    internal fun produserKafkamelding(
        søknad: KomplettSøknad,
        metadata: Metadata
    ) {
        if (metadata.version != 1) throw IllegalStateException("Kan ikke legge søknad med versjon ${metadata.version} til prosessering.")

        val recordMetaData = produsent.send(
            ProducerRecord(
                OMD_ALENEOMSORG_MOTTATT_TOPIC.name,
                søknad.søknadId,
                TopicEntry(
                    metadata = metadata,
                    data = JSONObject(søknad.somJson())
                )
            )
        ).get()
        logger.info(formaterStatuslogging(søknad.søknadId, "sendes til topic ${OMD_ALENEOMSORG_MOTTATT_TOPIC.name} med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'"))
    }

    override suspend fun check(): Result {
        return try {
            produsent.partitionsFor(OMD_ALENEOMSORG_MOTTATT_TOPIC.name)
            Healthy(NAME, "Tilkobling til Kafka OK!")
        } catch (cause: Throwable) {
            logger.error("Feil ved tilkobling til Kafka", cause)
            UnHealthy(NAME, "Feil ved tilkobling mot Kafka. ${cause.message}")
        }
    }
}

private class SøknadSerializer : Serializer<TopicEntry<JSONObject>> {
    override fun serialize(topic: String, data: TopicEntry<JSONObject>) : ByteArray {
        val metadata = JSONObject()
            .put("correlationId", data.metadata.correlationId)
            .put("version", data.metadata.version)

        return JSONObject()
            .put("metadata", metadata)
            .put("data", data.data)
            .toString()
            .toByteArray()
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}
