package kafkaessink.domain

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

object KafkaSettings {
  def consumerSettings(config: KafkaConfig): IO[ConsumerSettings[IO, String, ClickRecord]] =
    Avro.avroSettingsSharedClient(config.schemaRegistryUrl).map {
      avroSettings =>

        given Resource[IO, ValueDeserializer[IO, ClickRecord]] =
          avroDeserializer[ClickRecord].forValue(avroSettings)

        ConsumerSettings[IO, String, ClickRecord]
          .withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withBootstrapServers(config.bootstrapServers)
          .withGroupId(config.groupId)
          .withEnableAutoCommit(false)
    }
}
