import io.kotest.core.config.AbstractProjectConfig

class ProjectConfig : AbstractProjectConfig() {
    // currently breaks tests, see https://github.com/kotest/kotest-extensions-allure/issues/47
    // override fun extensions(): List<Extension> = listOf(AllureTestReporter())
}