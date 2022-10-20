package de.gma.gamma.q

import de.gma.gamma.builtins.GammaBaseScope
import de.gma.gamma.datatypes.Remark
import de.gma.gamma.datatypes.Value
import de.gma.gamma.datatypes.expressions.LetExpression
import de.gma.gamma.datatypes.expressions.SetExpression
import de.gma.gamma.datatypes.scope.ModuleScope
import de.gma.gamma.datatypes.scope.Scope
import de.gma.gamma.datatypes.values.VoidValue
import de.gma.gamma.parser.Parser
import java.io.*
import java.nio.charset.Charset
import java.util.*

private const val GMA_SOURCE = ".gma_source"

const val PROP_FILENAME = "project.properties"
const val PROP_NAME = "name"
const val PROP_BINDING_COUNT = "bindingCount"
const val PROP_BINDING_PREFIX = "binding"
const val PROP_SOURCE_PREFIX = "source"

class Context(var name: String, val folder: String = ".") {
    private val parentFolder = File(folder, GMA_SOURCE)

    private val scope = InteractiveScope(rootScope)
    private val output = ByteArrayOutputStream()

    private val sources = mutableMapOf<String, Int>()

    init {
        parentFolder.mkdirs()
        readContent()
    }

    fun execute(code: String): Pair<Value, String> {
        val sourceNumber = nextSourceNumber()
        val sourceName = getSourceName(sourceNumber)
        val parser = Parser(code, sourceName)
        val expression = parser.nextExpression() ?: return VoidValue.build() to ""

        if (parser.nextExpression() != null) {
            throw RuntimeException("Cannot have multiple expressions in one source")
        }

        if (expression is SetExpression) {
            throw RuntimeException("set expressions are not allowed, use let instead")
        }

        return if (expression is LetExpression)
            handleLetExpression(expression, sourceNumber, code)
        else
            handleExecuteExpression(expression)
    }

    fun getBindings() =
        scope.getAllNames().filter { it != "this" && it != "super" }

    fun getSourceForBinding(binding: String) =
        loadSource(sources[binding]) ?: ""

    fun getRemarkForBinding(binding: String) =
        scope.getRemark(binding)

    private fun handleLetExpression(
        expression: LetExpression,
        sourceNumber: Int,
        code: String
    ): Pair<Value, String> {
        val name = expression.identifier.name
        GammaBaseScope.output = PrintStream(output)
        if (getBindings().contains(name)) {
            val oldSource = sources[name]!!
            expression.evaluate(scope)
            sources.replace(name, sourceNumber)
            saveSource(sourceNumber, code)
            removeSourceFile(getSourceName(oldSource))
        } else {
            expression.evaluate(scope)
            sources[name] = sourceNumber
            saveSource(sourceNumber, code)
        }
        storeContent()
        val out = returnAndCloseOutput()
        return VoidValue.build() to out
    }

    private fun returnAndCloseOutput(): String {
        val out = output.toString(Charset.defaultCharset())!!
        GammaBaseScope.output.close()
        GammaBaseScope.output = System.out
        output.close()
        output.reset()
        return out
    }

    private fun storeContent() {
        Properties().apply {
            setProperty(PROP_NAME, name)
            setProperty(PROP_BINDING_COUNT, getBindings().size.toString())

            getBindings().forEachIndexed { i, binding -> setProperty("$PROP_BINDING_PREFIX.$i", binding) }

            getBindings().forEach { binding ->
                setProperty(
                    "$PROP_SOURCE_PREFIX.$binding",
                    sources[binding].toString()
                )
            }
        }.store(FileWriter(File(parentFolder, PROP_FILENAME)), "")
    }

    private fun readContent() {
        val propFile = File(parentFolder, PROP_FILENAME)
        if (propFile.exists() && !propFile.isDirectory) {
            val props = Properties().apply { load(FileReader(propFile)) }
            try {
                name = props.getProperty(PROP_NAME)
                val count = props.getProperty(PROP_BINDING_COUNT).toIntOrNull() ?: 0
                if (count > 0) {
                    (0 until count).forEach {
                        val binding = props.getProperty("$PROP_BINDING_PREFIX.$it")
                        val source = props.getProperty("$PROP_SOURCE_PREFIX.$binding").toIntOrNull() ?: -1
                        sources[binding] = source
                        val code = loadSource(source)
                        if (code != null) evaluate(code)
                    }
                }
            } catch (e: Exception) {
                throw java.lang.RuntimeException("invalid project file", e)
            }
        }
    }

    private fun evaluate(code: String) {
        Parser(code).nextExpression()?.evaluate(scope)
    }

    private fun removeSource(number: Int?) =
        if (number == null) null
        else File(parentFolder, getSourceName(number)).delete()

    private fun saveSource(number: Int, content: String) =
        File(parentFolder, getSourceName(number)).writeText(content)

    private fun loadSource(number: Int?) =
        if (number == null) null
        else File(parentFolder, getSourceName(number)).readText()

    private fun getSourceName(number: Int) =
        "$number.gma"

    private fun removeSourceFile(name: String) =
        File(parentFolder, name).delete()

    private fun handleExecuteExpression(expression: Value): Pair<Value, String> {
        GammaBaseScope.output = PrintStream(output)
        val retVal = expression.evaluate(scope)
        storeContent()
        val retOut = returnAndCloseOutput()

        return retVal to retOut
    }

    private fun nextSourceNumber() =
        (sources
            .values.maxOfOrNull { it } ?: 0) + 1

    fun removeBinding(name: String) {
        scope.removeBinding(name)
        val codeNumber = sources[name] ?: return
        removeSource(codeNumber)
        sources.remove(name)
        storeContent()
    }

    companion object {
        private val rootScope = GammaBaseScope
    }

    private inner class InteractiveScope(parent: Scope? = null) : ModuleScope("", parent) {
        override fun bindValue(name: String, value: Value, documentation: Remark?, strict: Boolean) {
            super.bindValue(name, value, documentation, false)
        }

        fun removeBinding(name: String) {
            content.remove(name)
            remarks.remove(name)
        }
    }

}
