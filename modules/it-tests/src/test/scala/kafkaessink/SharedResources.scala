package kafkaessink

import kafkaessink.infrastructure.ElasticsearchService
import kafkaessink.service.KafkaElasticSinkService
import weaver.*
import cats.effect.IO
import cats.effect.kernel.Resource
import com.dimafeng.testcontainers.DockerComposeContainer
import java.io.File
import com.dimafeng.testcontainers.ExposedService
import com.dimafeng.testcontainers.ServiceLogConsumer
import kafkaessink.config.AppConfig
import kafkaessink.config.KafkaConfig
import kafkaessink.config.ElasticsearchConfig
import fs2.kafka.KafkaAdminClient
import fs2.kafka.AdminClientSettings
import org.apache.kafka.clients.admin.NewTopic
import kafkaessink.domain.Avro
import kafkaessink.domain.ClickRecord
import org.testcontainers.containers.wait.strategy.Wait
import com.dimafeng.testcontainers.WaitingForService

final case class SharedResources(
  elasticsearchService: ElasticsearchService,
  kafkaElasticSinkService: KafkaElasticSinkService,
  config: AppConfig
)

object SharedResources extends GlobalResource {
  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] = {
    for {
      appConfig <- AppConfig.load.toResource
      elasticSearchService <- ElasticsearchService.resource(
        appConfig.elasticsearch,
        appConfig.kafka.topic
      )
      kafkaElasticSinkService = KafkaElasticSinkService(
        appConfig.kafka,
        elasticSearchService
      )

      kafkaAdminClient <- KafkaAdminClient.resource[IO](
        AdminClientSettings(appConfig.kafka.bootstrapServers)
      )
      topicsName <- kafkaAdminClient.listTopics.names.toResource
      _ <- IO
        .whenA(!topicsName(appConfig.kafka.topic))(
          kafkaAdminClient
            .createTopic(new NewTopic(appConfig.kafka.topic, 3, 1.toShort))
        )
        .toResource

      avroSettingsSharedClient <- Avro
        .avroSettingsSharedClient(appConfig.kafka.schemaRegistryUrl)
        .toResource
      _ <- avroSettingsSharedClient
        .registerSchema[String]("click-record-key")
        .toResource
      _ <- avroSettingsSharedClient
        .registerSchema[ClickRecord]("click-record-value")
        .toResource

      _ <- global.putR(
        SharedResources(
          elasticSearchService,
          kafkaElasticSinkService,
          appConfig
        )
      )

    } yield ()
  }
}
