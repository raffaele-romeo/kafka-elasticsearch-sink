package kafkaessink.domain

import cats.effect.IO
import fs2.kafka.vulcan.SchemaRegistryClientSettings
import fs2.kafka.vulcan.AvroSettings
import fs2.kafka.vulcan.Auth

object Avro {
  def avroSettingsSharedClient(schemaRegistryUrl: String): IO[AvroSettings[IO]] =
    SchemaRegistryClientSettings[IO](schemaRegistryUrl)
      .withAuth(Auth.None)
      .createSchemaRegistryClient
      .map(AvroSettings[IO](_).withAutoRegisterSchemas(false))
}
