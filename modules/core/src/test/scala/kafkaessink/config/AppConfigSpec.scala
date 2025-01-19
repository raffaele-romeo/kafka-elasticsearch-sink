package kafkaessink.config

import weaver.SimpleIOSuite

object AppConfigSpec extends SimpleIOSuite {
  test("AppConfig.load can read a AppConfig from application.conf") {
    AppConfig.load.as(success)
  }
}
