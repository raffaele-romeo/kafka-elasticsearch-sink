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
  private val FastDataDevServiceName   = "fast-data-dev"
  private val ElasticsearchServiceName = "elasticsearch"
  private val KafkaTopicName           = "website-clicks"

  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] = {
    for {
      dockerCompose <- startDockerCompose()
      mappedSchemaRegistryServiceHost = dockerCompose.getServiceHost(
        FastDataDevServiceName,
        8081
      )
      mappedSchemaRegistryServicePort = dockerCompose.getServicePort(
        FastDataDevServiceName,
        8081
      )

      mappedKafkaServiceHost = dockerCompose.getServiceHost(
        FastDataDevServiceName,
        9092
      )
      mappedKafkaServicePort = dockerCompose.getServicePort(
        FastDataDevServiceName,
        9092
      )

      mappedElasticsearchServiceHost = dockerCompose.getServiceHost(
        ElasticsearchServiceName,
        9200
      )
      mappedElasticsearchServicePort = dockerCompose.getServicePort(
        ElasticsearchServiceName,
        9200
      )

      appConfig = AppConfig(
        kafka = KafkaConfig(
          bootstrapServers = s"$mappedKafkaServiceHost:$mappedKafkaServicePort",
          schemaRegistryUrl =
            s"http://$mappedSchemaRegistryServiceHost:$mappedSchemaRegistryServicePort",
          groupId = "es-sink-group",
          topic = KafkaTopicName
        ),
        elasticsearch = ElasticsearchConfig(
          mappedElasticsearchServiceHost,
          mappedElasticsearchServicePort
        )
      )

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
      _ <- kafkaAdminClient
        .createTopic(new NewTopic(KafkaTopicName, 3, 1.toShort))
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

  private val dockerComposeDef: DockerComposeContainer.Def = {
    DockerComposeContainer.Def(
      composeFiles =
        DockerComposeContainer.fileToEither(File("../../docker-compose.yml")),
      exposedServices = Seq(
        ExposedService(FastDataDevServiceName, 8081),
        ExposedService(FastDataDevServiceName, 9092),
        ExposedService(ElasticsearchServiceName, 9200)
      ),
      logConsumers = Seq(
        ServiceLogConsumer(
          FastDataDevServiceName,
          line => println(line.getUtf8String)
        )
      )
    )
  }

  private def startDockerCompose(): Resource[IO, DockerComposeContainer] =
    Resource
      .make(IO.blocking(dockerComposeDef.start())) { container =>
        IO.blocking(container.stop())
      }

}
