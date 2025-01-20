package kafkaessink.service

import weaver.*
import java.time.Instant
import org.scalacheck.Gen
import kafkaessink.generators.ClickRecordGenerators
import cats.effect.kernel.Resource
import cats.effect.IO
import kafkaessink.domain.ClickRecord
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerRecord
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow
import fs2.Chunk
import kafkaessink.SharedResources
import kafkaessink.KafkaProducerSettings
import scala.concurrent.duration.*

final class KafkaElasticSinkServiceSpec(global: GlobalRead) extends IOSuite {
  override type Res = SharedResources

  override def sharedResource: Resource[IO, Res] =
    global.getOrFailR[SharedResources](None)

  test(
    "KafkaElasticSinkService.run should consume records from Kafka topic and write them to Elasticsearch"
  ) { deps =>
    val records =
      Gen.listOfN(10, ClickRecordGenerators.clickRecordGen).sample.get

    val producerRecords = records.map(record =>
      ProducerRecord(deps.config.kafka.topic, record.sessionId, record)
    )

    for {
      fiber         <- deps.kafkaElasticSinkService.run.start
      producerSetting <- KafkaProducerSettings.producerSettings(
        deps.config.kafka
      )
      _ <- KafkaProducer[IO]
        .stream(producerSetting)
        .evalMap(producer =>
          producer.produce(Chunk.from(producerRecords)).as(CommitNow)
        )
        .compile
        .drain
      nowInstant <- IO.realTimeInstant 
      _ <- IO.sleep(4.seconds) 
      result <- deps.elasticsearchService.query(
        nowInstant
      )
      _ <- fiber.cancel
    } yield expect(result.size == 10)

  }
}
