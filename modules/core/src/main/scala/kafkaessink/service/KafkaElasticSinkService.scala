package kafkaessink.service

import cats.syntax.all.*
import kafkaessink.config.AppConfig
import kafkaessink.infrastructure.ElasticsearchService
import fs2.kafka.KafkaConsumer
import kafkaessink.domain.KafkaSettings
import kafkaessink.domain.ClickRecord
import cats.effect.IO
import fs2.Stream
import fs2.Chunk
import fs2.kafka.ConsumerRecord
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow
import org.typelevel.log4cats.LoggerFactory
import kafkaessink.logging.LoggerFactory.given
import kafkaessink.config.KafkaConfig

class KafkaElasticSinkService(
  kafkaConfig: KafkaConfig,
  elasticsearchService: ElasticsearchService
) {
  private val logger = LoggerFactory[IO].getLogger

  def run: IO[Unit] = KafkaSettings
    .consumerSettings(kafkaConfig)
    .flatMap { consumerSettings =>
      logger.info("Starting kafka consumer") *>
        KafkaConsumer[IO]
          .stream(consumerSettings)
          .subscribeTo(kafkaConfig.topic)
          .consumeChunk(processRecords)
    }

  private def processRecords(
    records: Chunk[ConsumerRecord[String, ClickRecord]]
  ): IO[CommitNow] =
    IO.realTimeInstant.flatMap(instantNow =>
      elasticsearchService
        .index(instantNow, records.map(_.value).toList)
        .as(CommitNow)
    )
}
