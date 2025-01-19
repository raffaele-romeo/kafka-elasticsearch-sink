package kafkaessink.logging

import cats.effect.IO
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.LoggerFactory

object LoggerFactory {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
}
