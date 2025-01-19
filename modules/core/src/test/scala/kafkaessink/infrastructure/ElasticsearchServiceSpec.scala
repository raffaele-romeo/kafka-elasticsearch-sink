package kafkaessink.infrastructure

import weaver.SimpleIOSuite
import java.time.Instant
import cats.effect.IO

object ElasticsearchServiceSpec extends SimpleIOSuite {
  test(
    "ElasticsearchService.getIndexName should output the correct index name"
  ) {
    val instant = Instant.parse("2025-01-01T14:17:10Z")

    IO(
      expect(
        ElasticsearchService.getIndexName(
          instant,
          "topic-name"
        ) == "topic-name_2025-01-01-14-15"
      )
    )
  }
}
