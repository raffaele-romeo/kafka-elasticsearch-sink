package kafkaessink.config

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class KafkaConfig(
  bootstrapServers: String,
  schemaRegistryUrl: String,
  topic: String,
  groupId: String
) derives ConfigReader