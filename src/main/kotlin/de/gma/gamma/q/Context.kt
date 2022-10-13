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
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

private const val GMA_SOURCE = ".gma_source"

class Context(var name: String, val folder: String = ".") {
    private val parentFolder = File(folder, GMA_SOURCE)

    private val scope = InteractiveScope(rootScope)
    private var codeNumber = 0

    private val bindings = mutableListOf<String>()

    private val sources = mutableMapOf<String, String>()

    init {
        parentFolder.mkdirs()
        readContent()
    }

    fun execute(code: String): Value {
        val sourceName = nextSourceName()
        val parser = Parser(code, sourceName)
        val expression = parser.nextExpression() ?: return VoidValue.build()

        if (parser.nextExpression() != null) {
            throw RuntimeException("Cannot have two expressions in one source")
        }

        if (expression is SetExpression) {
            throw RuntimeException("set expressions are not allowed, use let instead")
        }

        return if (expression is LetExpression)
            handleLetExpression(expression, sourceName, code)
        else
            handleWatchExpression(expression)
    }

    fun getBindings() =
        bindings.toList()

    fun getSourceForBinding(binding: String) =
        loadSource(sources[binding]) ?: ""

    private fun handleLetExpression(
        expression: LetExpression,
        sourceName: String,
        code: String
    ): Value {
        val name = expression.identifier.name
        if (bindings.contains(name)) {
            val oldSource = sources[name]!!
            expression.evaluate(scope)
            sources.replace(name, sourceName)
            saveSource(sourceName, code)
            removeSource(oldSource)
        } else {
            expression.evaluate(scope)
            bindings.add(name)
            sources[name] = sourceName
            saveSource(sourceName, code)
        }
        storeContent()
        return VoidValue.build()
    }

    private fun storeContent() {
        Properties().apply {
            setProperty("name", name)
            setProperty("bindingCount", bindings.size.toString())

            bindings.forEachIndexed { i, binding -> setProperty("binding.$i", binding) }

            bindings.forEach { binding -> setProperty("source.$binding", sources[binding]) }
        }.store(FileWriter(File(parentFolder, "project.properties")), "")
    }

    private fun readContent() {
        val propFile = File(parentFolder, "project.properties")
        if (propFile.exists() && !propFile.isDirectory) {
            val props = Properties().apply { load(FileReader(propFile)) }
            name = props.getProperty("name")
            val count = props.getProperty("bindingCount").toIntOrNull() ?: 0
            if (count > 0) {
                (0 until count).forEach {
                    val binding = props.getProperty("binding.$it")
                    val source = props.getProperty("source.$binding")
                    bindings.add(binding)
                    sources[binding] = source
                }
            }
        }
    }

    private fun saveSource(name: String, content: String) =
        File(parentFolder, name).writeText(content)

    private fun loadSource(name: String?) =
        if (name == null) null
        else File(parentFolder, name).readText()

    private fun removeSource(name: String) =
        File(parentFolder, name).delete()

    private fun handleWatchExpression(expression: Value) =
        expression.evaluate(scope)

    private fun nextSourceName() =
        "${codeNumber++}.gma"

    companion object {
        val rootScope = GammaBaseScope
    }

    inner class InteractiveScope(parent: Scope? = null) : ModuleScope("", parent) {
        override fun bindValue(name: String, value: Value, documentation: Remark?, strict: Boolean) {
            super.bindValue(name, value, documentation, false)
        }
    }

}
