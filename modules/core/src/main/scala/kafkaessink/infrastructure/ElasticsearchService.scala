package kafkaessink.infrastructure

import cats.effect.Resource
import com.sksamuel.elastic4s.{ ElasticClient, ElasticProperties }
import com.sksamuel.elastic4s.cats.effect.instances.*
import com.sksamuel.elastic4s.ElasticDsl.*
import kafkaessink.domain.ClickRecord
import kafkaessink.config.ElasticsearchConfig
import java.time.format.DateTimeFormatter
import java.time.{ Instant, ZoneId }
import com.sksamuel.elastic4s.circe.*
import io.circe.generic.auto.*
import com.sksamuel.elastic4s.http.JavaClient
import cats.syntax.all.*
import cats.effect.IO
import kafkaessink.logging.LoggerFactory.given
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.DurationInt

trait ElasticsearchService {
  def index(instant: Instant, records: List[ClickRecord]): IO[Unit]
  // Only used for testing
  def query(instant: Instant): IO[List[ClickRecord]]
}

class ElasticsearchServiceLive(
  client: ElasticClient,
  topicName: String
) extends ElasticsearchService {
  private val logger = LoggerFactory[IO].getLogger

  override def index(instant: Instant, records: List[ClickRecord]): IO[Unit] = {
    val indexName = ElasticsearchService.getIndexName(instant, topicName)

    client
      .execute {
        bulk(
          records.map(makeIndexRequest(indexName))
        ).refreshImmediately
      }
      .flatMap(response =>
        if response.isError then
          logger.error(s"Failed to index documents: ${response.error}")
        else logger.info(s"Successfully indexed ${records.size} documents into $indexName")
      )
      .onError(e => logger.error(e)(s"Failed to index documents"))
  }

  override def query(instant: Instant): IO[List[ClickRecord]] = {
    val indexName = ElasticsearchService.getIndexName(instant, topicName)

    client
      .execute {
        search(indexName).matchAllQuery()
      }
      .map(response => response.result.to[ClickRecord].toList)
  }

  private def makeIndexRequest(indexName: String)(record: ClickRecord) = {
    indexInto(indexName)
      .doc(record)
      .refreshImmediately
  }
}

object ElasticsearchService {
  def resource(
    elasticsearchConfig: ElasticsearchConfig,
    kafkaTopic: String
  ): Resource[IO, ElasticsearchService] = {
    val props = ElasticProperties(
      s"http://${elasticsearchConfig.host}:${elasticsearchConfig.port}"
    )
    Resource
      .fromAutoCloseable(
        IO.delay(ElasticClient(JavaClient(props)))
      )
      .map(client => new ElasticsearchServiceLive(client, kafkaTopic))
  }

  private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd-HH-mm")
    .withZone(ZoneId.systemDefault())

  private val BucketDuratioinSeconds: Long = 15.minutes.toSeconds

  def getIndexName(instant: Instant, topicName: String): String = {
    val instantEpochSec = instant.getEpochSecond
    val adjustedInstant = Instant
      .ofEpochSecond(
        instantEpochSec - (instantEpochSec % BucketDuratioinSeconds)
      )
      .atZone(ZoneId.systemDefault())

    s"${topicName}_${adjustedInstant.format(dateTimeFormatter)}"
  }
}
