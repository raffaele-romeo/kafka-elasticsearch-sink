package kafkaessink.generators

import org.scalacheck.Gen
import kafkaessink.domain.ClickRecord

object ClickRecordGenerators {
  private def optionalGen[T](gen: Gen[T]): Gen[Option[T]] =
    Gen.option(gen)

  private val sessionIdGen: Gen[String] = Gen.uuid.map(_.toString)
  private val browserGen: Gen[String] =
    Gen.oneOf("Chrome", "Firefox", "Safari", "Edge", "Opera")
  private val campaignGen: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)
  private val channelGen: Gen[String] =
    Gen.oneOf("email", "social", "direct", "organic", "paid")
  private val referrerGen: Gen[String] = Gen.oneOf(
    "https://google.com",
    "https://facebook.com",
    "https://twitter.com"
  )
  private val ipGen: Gen[String] = for {
    a <- Gen.choose(0, 255)
    b <- Gen.choose(0, 255)
    c <- Gen.choose(0, 255)
    d <- Gen.choose(0, 255)
  } yield s"$a.$b.$c.$d"

  val clickRecordGen: Gen[ClickRecord] = for {
    sessionId <- sessionIdGen
    browser   <- optionalGen(browserGen)
    campaign  <- optionalGen(campaignGen)
    channel   <- channelGen
    referrer  <- optionalGen(referrerGen)
    ip        <- optionalGen(ipGen)
  } yield ClickRecord(sessionId, browser, campaign, channel, referrer, ip)
}
