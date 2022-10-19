package de.gma.gamma.q

import de.gma.gamma.datatypes.values.VoidValue
import de.gma.gamma.parser.EvaluationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

private const val TEST_PROJECT_NAME = "TEST"
private const val TEST_LOAD_PROJECT_NAME = "TEST_LOAD"
private const val TEST_ERROR_PROJECT_NAME = "ERROR"

class ContextTest {

    private val baseFolderName = "./target/ContextTest/Context"
    private val projectFolder = File(baseFolderName, ".gma_source")

    private lateinit var ctx: Context

    @BeforeEach
    fun setup() {
        File(baseFolderName).mkdirs()
        ctx = Context(TEST_PROJECT_NAME, baseFolderName)
    }

    @AfterEach
    fun cleanUp() {
        File(baseFolderName).deleteRecursively()
    }

    @Nested
    inner class CleanProjectTests {

        @Test
        fun `should return its name`() {
            assertThat(ctx.name).isEqualTo(TEST_PROJECT_NAME)
        }

        @Test
        fun `should return empty bindings list`() {
            assertThat(ctx.getBindings()).isEmpty()
        }

        @Test
        fun `should evaluate a simple expression`() {
            assertThat(ctx.execute("10").first.toInteger().longValue).isEqualTo(10L)
        }

        @Test
        fun `should create a new binding`() {
            ctx.execute("let a = 10")
            assertThat(ctx.getBindings()).containsExactly("a")
            assertThat(ctx.execute("a").first.toInteger().longValue).isEqualTo(10L)
        }

        @Test
        fun `should remove a binding`() {
            ctx.execute("let a = 10")
            ctx.removeBinding("a")
            assertThat(ctx.getBindings()).doesNotContain("a")
            assertThatThrownBy {
                ctx.execute("a")
            }.isInstanceOf(EvaluationException::class.java)
                .hasMessage("id a is undefined.")
        }

        @Test
        fun `should return void if no expression in code`() {
            assertThat(ctx.execute("").first).isEqualTo(VoidValue.build())
        }

        @Test
        fun `should throw exception if more that one expression in code`() {
            assertThatThrownBy {
                ctx.execute(
                    """
                let a = 10
                let b = 20
            """.trimIndent()
                )
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Cannot have multiple expressions in one source")
        }

        @Test
        fun `should not allow set expressions`() {
            assertThatThrownBy {
                ctx.execute("set a! = 10")
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessage("set expressions are not allowed, use let instead")
        }

        @Test
        fun `should return the code for a binding`() {
            val code = "let a = 10"
            ctx.execute(code)
            assertThat(ctx.getSourceForBinding("a")).isEqualTo(code)
        }

        @Test
        fun `should throw exception if code has errors`() {
            assertThatThrownBy {
                ctx.execute("10 +")
            }.isInstanceOf(EvaluationException::class.java)
                .hasMessage("Illegal end of expression")
        }

        @Test
        fun `should throw exception if binding has errors`() {
            assertThatThrownBy {
                ctx.execute("let a = + 10")
            }.isInstanceOf(EvaluationException::class.java)
                .hasMessage("Illegal Token +")
            assertThat(ctx.getBindings()).isEmpty()
        }
    }

    @Nested
    inner class StoreProjectTests {

        @Test
        fun `should store a first binding in code 1`() {
            ctx.execute("let a = 10")

            val codeFile = File(projectFolder, "1.gma")

            assertThat(codeFile).exists()
            assertThat(codeFile).hasContent("let a = 10")

            assertProjectProperties(listOf("a" to 1))
        }

        @Test
        fun `should remove the code when removing a binding`() {
            ctx.execute("let a = 10")
            ctx.removeBinding("a")

            val codeFile = File(projectFolder, "1.gma")

            assertProjectProperties(emptyList())
            assertThat(codeFile).doesNotExist()
        }

        @Test
        fun `should store a second binding in code 2`() {
            ctx.execute("let a = 10")
            ctx.execute("let b = 20")

            val codeFile1 = File(projectFolder, "1.gma")
            val codeFile2 = File(projectFolder, "2.gma")

            assertThat(codeFile1).exists()
            assertThat(codeFile2).exists()
            assertThat(codeFile2).hasContent("let b = 20")

            assertProjectProperties(listOf("a" to 1, "b" to 2))
        }

        @Test
        fun `should store a changed binding in a new code file`() {
            ctx.execute("let a = 10")
            ctx.execute("let a = 20")

            val codeFile1 = File(projectFolder, "1.gma")
            val codeFile2 = File(projectFolder, "2.gma")

            assertThat(codeFile1).doesNotExist()
            assertThat(codeFile2).exists()
            assertThat(codeFile2).hasContent("let a = 20")

            assertProjectProperties(listOf("a" to 2))
        }

        @Test
        fun `should not store code that does not contain a binding`() {
            ctx.execute("10")

            assertThat(projectFolder.listFiles()!!.map { it.name }).containsExactly(PROP_FILENAME)
            assertProjectProperties(emptyList())
        }

        @Test
        fun `should not increase code number for code that does not contain a binding`() {
            ctx.execute("let a = 10")
            ctx.execute("a")
            ctx.execute("let b = 20")

            val projectFile = File(projectFolder, PROP_FILENAME)
            val codeFile1 = File(projectFolder, "1.gma")
            val codeFile2 = File(projectFolder, "2.gma")

            assertThat(projectFolder.listFiles()).hasSize(3)
            assertThat(projectFile).exists()
            assertThat(codeFile1).exists().hasContent("let a = 10")
            assertThat(codeFile2).exists().hasContent("let b = 20")

            assertProjectProperties(listOf("a" to 1, "b" to 2))
        }

        private fun assertProjectProperties(list: List<Pair<String, Int>>) {
            val props = Properties().apply {
                load(FileReader(File(projectFolder, PROP_FILENAME)))
            }
            assertThat(props.getProperty(PROP_NAME)).isEqualTo(TEST_PROJECT_NAME)
            assertThat(props.getProperty(PROP_BINDING_COUNT).toInt()).isEqualTo(list.size)
            list.forEachIndexed { index, pair ->
                assertThat(props.getProperty("$PROP_BINDING_PREFIX.$index")).isEqualTo(pair.first)
                assertThat(props.getProperty("$PROP_SOURCE_PREFIX.${pair.first}").toInt()).isEqualTo(pair.second)
            }
        }
    }

    @Nested
    inner class LoadProjectTest {

        @BeforeEach
        fun setup() {
            projectFolder.mkdirs()
        }

        @Test
        fun `should load a correct project with 1 source`() {
            setupProject(1)
            val ctx = Context("", baseFolderName)

            assertThat(ctx.name).isEqualTo(TEST_LOAD_PROJECT_NAME)
            assertThat(ctx.getBindings()).hasSize(1)
            assertThat(ctx.execute("a").first.toInteger().longValue).isEqualTo(10L)
        }

        @Test
        fun `should load a correct project with 2 sources`() {
            setupProject(2)
            val ctx = Context("", baseFolderName)

            assertThat(ctx.name).isEqualTo(TEST_LOAD_PROJECT_NAME)
            assertThat(ctx.getBindings()).hasSize(2)
            assertThat(ctx.execute("a").first.toInteger().longValue).isEqualTo(10L)
            assertThat(ctx.execute("b").first.toInteger().longValue).isEqualTo(20L)
        }

        @Test
        fun `should work with a project with 3 sources`() {
            setupProject(3)

            val ctx = Context("", baseFolderName)

            assertThat(ctx.execute("a + b + c").first.toInteger().longValue).isEqualTo(60L)
        }

        @Test
        fun `should work with a project without source`() {
            setupProject(0)

            val ctx = Context("", baseFolderName)

            assertThat(ctx.getBindings()).hasSize(0)
        }

        @Test
        fun `should create empty project when folder has no project file`() {
            val ctx = Context("BLA", baseFolderName)
            assertThat(ctx.getBindings()).hasSize(0)
            assertThat(ctx.name).isEqualTo("BLA")
        }

        @Test
        fun `should throw exception when name is missing in propFile`() {
            createPropFile(emptyMap())

            assertInvalidProjectFile(
                NullPointerException::class.java,
                "props.getProperty(PROP_NAME) must not be null"
            )
        }

        @Test
        fun `should throw exception when count is missing`() {
            createPropFile(
                mapOf(
                    PROP_NAME to TEST_ERROR_PROJECT_NAME
                )
            )

            assertInvalidProjectFile(
                NullPointerException::class.java,
                "props.getProperty(PROP_BINDING_COUNT) must not be null"
            )
        }

        @Test
        fun `should throw exception when binding is smaller than count`() {
            createPropFile(
                mapOf(
                    PROP_NAME to TEST_ERROR_PROJECT_NAME,
                    PROP_BINDING_COUNT to "1"
                )
            )

            assertInvalidProjectFile(
                NullPointerException::class.java,
                "props.getProperty(\"\$PROP_SOURCE_PREFIX.\$binding\") must not be null"
            )
        }

        @Test
        fun `should throw exception when source is missing`() {
            createPropFile(
                mapOf(
                    PROP_NAME to TEST_ERROR_PROJECT_NAME,
                    PROP_BINDING_COUNT to "1",
                    "$PROP_BINDING_PREFIX.0" to "a"
                )
            )

            assertInvalidProjectFile(
                NullPointerException::class.java,
                "props.getProperty(\"\$PROP_SOURCE_PREFIX.\$binding\") must not be null"
            )
        }

        @Test
        fun `should throw exception when source is not compiling`() {
            createPropFile(
                mapOf(
                    PROP_NAME to TEST_ERROR_PROJECT_NAME,
                    PROP_BINDING_COUNT to "1",
                    "$PROP_BINDING_PREFIX.0" to "a",
                    "$PROP_SOURCE_PREFIX.a" to "1"
                )
            )
            File(projectFolder, "1.gma").writeText("xxx")

            assertInvalidProjectFile(
                EvaluationException::class.java,
                "id xxx is undefined."
            )
        }

        private fun setupProject(numCode: Int) {
            val props = Properties().apply {
                setProperty(PROP_NAME, TEST_LOAD_PROJECT_NAME)
                setProperty(PROP_BINDING_COUNT, numCode.toString())
            }

            (0 until numCode).forEach {
                val bindName = ('a' + it).toString()
                val sourceName = "${it + 1}.gma"
                File(projectFolder, sourceName).writeText("let $bindName = ${it + 1}0")
                props.setProperty("$PROP_BINDING_PREFIX.$it", bindName)
                props.setProperty("$PROP_SOURCE_PREFIX.$bindName", (it + 1).toString())
            }

            props.store(FileWriter(File(projectFolder, PROP_FILENAME)), "")
        }

        private fun createPropFile(initProps: Map<String, String>) {
            val props = Properties().apply {
                putAll(initProps)
            }

            props.store(FileWriter(File(projectFolder, PROP_FILENAME)), "")
        }

        private fun assertInvalidProjectFile(causeClass: Class<out Exception>, message: String) {
            assertThatThrownBy {
                Context("", baseFolderName)
            }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("invalid project file")
                .hasCauseInstanceOf(causeClass)
                .hasRootCauseMessage(message)
        }
    }

    @Nested
    inner class OutputTest {

        @Test
        fun `shall collect output`() {
            val result = ctx.execute("print* 10")
            assertThat(result.second).isEqualTo("10")
        }

        @Test
        fun `shall collect no output`() {
            val result = ctx.execute("10")
            assertThat(result.second).isEmpty()
        }

        @Test
        fun `shall collect no duplicate output`() {
            val result = ctx.execute("print* 10")
            assertThat(result.second).isEqualTo("10")

            val result2 = ctx.execute("10")
            assertThat(result2.second).isEmpty()
        }
    }
}
