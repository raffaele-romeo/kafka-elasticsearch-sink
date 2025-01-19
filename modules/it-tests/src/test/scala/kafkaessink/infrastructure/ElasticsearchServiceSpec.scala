package kafkaessink.infrastructure

import weaver.*
import kafkaessink.SharedResources
import cats.effect.kernel.Resource
import cats.effect.IO
import java.time.Instant
import kafkaessink.generators.ClickRecordGenerators
import org.scalacheck.Gen

final class ElasticsearchServiceSpec(global: GlobalRead) extends IOSuite {
  override type Res = SharedResources

  override def sharedResource: Resource[IO, Res] =
    global.getOrFailR[SharedResources](None)

  override def maxParallelism = 1

  test(
    "ElasticsearchServiceLive.index should write records to the correct index"
  ) { deps =>
    val instant = Instant.parse("2025-01-01T14:17:10Z")

    val records =
      Gen.listOfN(10, ClickRecordGenerators.clickRecordGen).sample.get

    for {
      _      <- deps.elasticsearchService.index(instant, records)
      result <- deps.elasticsearchService.query(instant)
    } yield expect(result.size == 10)
  }
}
