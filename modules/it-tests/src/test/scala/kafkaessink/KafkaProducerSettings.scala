package kafkaessink

import cats.effect.{ IO, IOApp }
import fs2.kafka.vulcan.{ Auth, AvroSettings, SchemaRegistryClientSettings }
import fs2.*
import fs2.kafka.*
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow
import kafkaessink.config.KafkaConfig
import kafkaessink.domain.ClickRecord
import cats.effect.kernel.Resource
import fs2.kafka.{ ValueDeserializer, ValueSerializer }
import fs2.kafka.vulcan.{ avroDeserializer, avroSerializer }
import kafkaessink.domain.Avro

object KafkaProducerSettings {
  def producerSettings(
    config: KafkaConfig
  ): IO[ProducerSettings[IO, String, ClickRecord]] =
    Avro.avroSettingsSharedClient(config.schemaRegistryUrl).map {
      avroSettings =>

        given Resource[IO, ValueSerializer[IO, ClickRecord]] =
          avroSerializer[ClickRecord].forValue(avroSettings)

        ProducerSettings[IO, String, ClickRecord]
          .withBootstrapServers(config.bootstrapServers)
    }
}
