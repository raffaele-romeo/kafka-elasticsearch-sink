package kafkaessink.domain

import cats.syntax.all.*
import vulcan.Codec
import cats.effect.IO

final case class ClickRecord(
  sessionId: String,
  browser: Option[String],
  campaign: Option[String],
  channel: String,
  referrer: Option[String],
  ip: Option[String]
)

object ClickRecord {
  given Codec[ClickRecord] = Codec.record(
    name = "ClickRecord",
    namespace = "kafkaessink"
  ) { field =>
    (
      field("session_id", _.sessionId),
      field("browser", _.browser),
      field("campaign", _.campaign),
      field("channel", _.channel),
      field("referrer", _.referrer),
      field("ip", _.ip)
    ).mapN(ClickRecord.apply)
  }
}
