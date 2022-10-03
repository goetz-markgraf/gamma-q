package de.gma.gamma.q

import de.gma.gamma.builtins.GammaBaseScope
import de.gma.gamma.datatypes.Remark
import de.gma.gamma.datatypes.Value
import de.gma.gamma.datatypes.expressions.LetExpression
import de.gma.gamma.datatypes.expressions.SetExpression
import de.gma.gamma.datatypes.scope.ModuleScope
import de.gma.gamma.datatypes.scope.Scope
import de.gma.gamma.parser.Parser
import java.io.File

private const val GMA_SOURCE = ".gma_source"

class Context(val name: String, val folder: String = ".") {
    private val parentFolder = File(folder, GMA_SOURCE)

    init {
        parentFolder.mkdirs()
    }

    private val scope = InteractiveScope(rootScope)
    private var codeNumber = 0

    private val bindings = mutableListOf<String>()

    private val sources = mutableMapOf<String, String>()

    fun execute(code: String) {
        val sourceName = nextSourceName()
        val parser = Parser(code, sourceName)
        val expression = parser.nextExpression() ?: return

        if (parser.nextExpression() != null) {
            throw RuntimeException("Cannot have two expressions in one source")
        }

        if (expression is SetExpression) {
            throw RuntimeException("set expressions are not allowed, use let instead")
        }

        if (expression is LetExpression) {
            handleLetExpression(expression, sourceName, code)
        } else
            handleWatchExpression(expression)
    }

    fun getBindings() =
        bindings.toList()

    private fun handleLetExpression(
        expression: LetExpression,
        sourceName: String,
        code: String
    ) {
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
    }

    private fun saveSource(name: String, content: String) =
        File(parentFolder, name).writeText(content)

    private fun removeSource(name: String) =
        File(parentFolder, name).delete()

    private fun handleWatchExpression(expression: Value) {
        val value = expression.evaluate(scope)
        throw RuntimeException(value.prettyPrint())
    }

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
