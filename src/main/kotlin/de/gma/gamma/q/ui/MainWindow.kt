package de.gma.gamma.q.ui

import de.gma.gamma.datatypes.values.VoidValue
import de.gma.gamma.parser.EvaluationException
import de.gma.gamma.q.Context
import de.gma.gamma.q.ui.swingutil.RasterLayout
import de.gma.gamma.q.ui.swingutil.WindowUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

class MainWindow(private val context: Context) : JFrame() {
    private val scopeListModel = DefaultListModel<String>()
    private val scopeList = createScopeList()
    private val watchListModel = DefaultListModel<String>()
    private val watchList = createWatchList()
    private val textPane = createEditor()
    private val outputPane = createErrorPane()
    private val editor = textPane.styledDocument

    init {
        title = "Gamma Q – ${context.name}(${context.folder})"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        contentPane = createContentPane()
    }

    fun open() {
        pack()
        WindowUtil.ensureMinimumSize(this)
        setLocationRelativeTo(null)
        refresh()
        isVisible = true
        textPane.requestFocus()
    }

    private fun refresh() {
        scopeList.clearSelection()
        scopeListModel.clear()
        scopeListModel.addAll(context.getBindings())
        editor.remove(0, editor.length)
        editor.insertString(0, "let ", null)
    }

    private fun createContentPane() =
        JPanel(BorderLayout()).apply {
            border = padding()
            add(
                JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    createBrowserPanel(),
                    createEditorPanel()
                ), BorderLayout.CENTER
            )
        }

    private fun createBrowserPanel() =
        JPanel(RasterLayout(50, 12)).apply {
            add("1 1", JLabel("Scope"))
            add("1 3 12 -1", JScrollPane(scopeList))

            add("15 1", JLabel("Watch"))
            add("15 3 12 -1", JScrollPane(watchList))
        }

    private fun createScopeList() =
        JList(scopeListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener { loadSource(selectedValue) }
        }

    private fun createWatchList() =
        JList(watchListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }


    private fun createEditorPanel() =
        JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            border = padding(2)
            leftComponent = JScrollPane(textPane)
            rightComponent = JScrollPane(outputPane)
        }

    private fun createEditor() =
        JTextPane().apply {
            font = Font("Serif", Font.PLAIN, 18)
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), "execute")
            actionMap.put("execute", ExecuteAction())
            preferredSize = Dimension(300, 150)
        }

    private fun createErrorPane() =
        JTextPane().apply {
            isEditable = false
            isEnabled = false
            preferredSize = Dimension(300, 150)
        }

    private fun loadSource(binding: String?) {
        if (binding == null) return
        editor.remove(0, editor.length)
        editor.insertString(0, context.getSourceForBinding(binding), null)
    }

    companion object {
        private fun padding(i: Int = 1) = BorderFactory.createEmptyBorder(5 * i, 5 * i, 5 * i, 5 * i)
    }

    // ==========================================================

    inner class ExecuteAction() :
        AbstractAction("Execute") {
        init {
            putValue(SHORT_DESCRIPTION, "executes the code in the editor pane")
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK))
        }

        override fun actionPerformed(e: ActionEvent?) {
            try {
                val result = context.execute(editor.getText(0, editor.length))
                refresh()
                if (result !is VoidValue)
                    addOutputLine(result.toString())
            } catch (e: EvaluationException) {
                addOutputLine("${e.message} (${e.source})")
            } catch (e: Exception) {
                addOutputLine("Error: ${e.message}")
            }
        }

        private fun addOutputLine(line: String) {
            val doc = outputPane.document
            doc.insertString(doc.length, "${line}\n", null)
        }
    }
}
