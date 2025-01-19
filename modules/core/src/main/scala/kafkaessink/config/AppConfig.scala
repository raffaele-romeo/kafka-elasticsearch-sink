package kafkaessink.config

import pureconfig.*
import pureconfig.generic.derivation.default.*
import cats.effect.IO
import org.typelevel.log4cats.Logger
import cats.effect.kernel.Sync
import cats.implicits.catsSyntaxApplicativeError
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import kafkaessink.logging.LoggerFactory.given

case class AppConfig(
  kafka: KafkaConfig,
  elasticsearch: ElasticsearchConfig
) derives ConfigReader

object AppConfig {
  private val logger = LoggerFactory[IO].getLogger
  
  def load = 
    IO.delay(ConfigSource.default.loadOrThrow[AppConfig])
    .onError(e => logger.error(e)(s"Error while loading the config file: $e"))
}