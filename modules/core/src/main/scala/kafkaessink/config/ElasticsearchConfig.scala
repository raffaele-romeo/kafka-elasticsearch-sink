package kafkaessink.config

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class ElasticsearchConfig(
  host: String,
  port: Int
) derives ConfigReader