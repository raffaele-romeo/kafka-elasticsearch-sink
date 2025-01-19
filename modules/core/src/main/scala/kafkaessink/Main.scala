package kafkaessink

import cats.effect.IOApp
import cats.effect.IO
import kafkaessink.config.AppConfig
import kafkaessink.infrastructure.ElasticsearchService
import kafkaessink.service.KafkaElasticSinkService
import cats.effect.kernel.Resource

object Main extends IOApp.Simple {
  override def run: IO[Unit] = makeResources.use(_.run)

  def makeResources: Resource[IO, KafkaElasticSinkService] =
    for {
      config <- AppConfig.load.toResource
      elasticsearchService <- ElasticsearchService.resource(
        config.elasticsearch,
        config.kafka.topic
      )
      kafkaElasticSinkService = KafkaElasticSinkService(
        config.kafka,
        elasticsearchService
      )
    } yield kafkaElasticSinkService
}
