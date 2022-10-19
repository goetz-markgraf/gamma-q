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
    private val executeAction = ExecuteAction()
    private val clearOutputAction = ClearOutputAction()
    private val removeBindingAction = RemoveBindingAction()

    init {
        title = "Gamma Q – ${context.name}(${context.folder})"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        contentPane = createContentPane()
        jMenuBar = createMenu()
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
        removeBindingAction.isEnabled = false
    }

    private fun createContentPane() =
        JPanel(BorderLayout()).apply {
            border = padding()
            add(
                JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    createBrowserPanel(),
                    createCodePanel()
                ), BorderLayout.CENTER
            )
        }

    private fun createBrowserPanel() =
        JPanel(RasterLayout(50, 15)).apply {
            add("1 1", JLabel("Scope"))
            add("1 3 12 -3", JScrollPane(scopeList))
            add("1 -1 3 2", SmallIconButton(removeBindingAction))

            add("15 1", JLabel("Watch"))
            add("15 3 12 -3", JScrollPane(watchList))
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


    private fun createCodePanel() =
        JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            leftComponent = createEditorPane()
            rightComponent = createOutputPane()
        }

    private fun createEditorPane() =
        JPanel(RasterLayout(10, 8)).apply {
            add("1 1 -5 -1", JScrollPane(textPane))
            add("-1 1 3 2", SmallIconButton(executeAction))
        }

    private fun createOutputPane() =
        JPanel(RasterLayout(10, 8)).apply {
            add("1 1 -5 -1", JScrollPane(outputPane))
            add("-1 1 3 2", SmallIconButton(clearOutputAction))
        }

    private fun createEditor() =
        JTextPane().apply {
            font = Font("Serif", Font.PLAIN, 18)
//            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), "execute")
//            actionMap.put("execute", ExecuteAction())
            preferredSize = Dimension(300, 150)
        }

    private fun createErrorPane() =
        JTextPane().apply {
            isEditable = false
            isEnabled = false
            preferredSize = Dimension(300, 150)
        }

    private fun loadSource(binding: String?) {
        if (binding == null) {
            removeBindingAction.isEnabled = false
            return
        }
        editor.remove(0, editor.length)
        editor.insertString(0, context.getSourceForBinding(binding), null)
        removeBindingAction.isEnabled = true
    }

    private fun createMenu() =
        JMenuBar().apply {
            add(JMenu("File").apply {
                add(JMenuItem(executeAction))
                add(JMenuItem(clearOutputAction))
                add(JMenuItem(removeBindingAction))
            })
        }

    companion object {
        private fun padding(i: Int = 1) = BorderFactory.createEmptyBorder(5 * i, 5 * i, 5 * i, 5 * i)
    }

    // ===================== Actions =====================================

    inner class ExecuteAction() :
        AbstractAction("Execute") {
        init {
            putValue(SMALL_ICON, ImageIcon(MainWindow::class.java.getResource("/images/execute.png")))
            putValue(SHORT_DESCRIPTION, "executes the code in the editor pane")
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK))
        }

        override fun actionPerformed(e: ActionEvent?) {
            try {
                val result = context.execute(editor.getText(0, editor.length))
                refresh()
                if (result.second.isNotBlank())
                    addOutputLine("===\n${result.second}===\n")
                if (result.first !is VoidValue)
                    addOutputLine(result.first.toString())
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

    inner class ClearOutputAction() : AbstractAction("Clear Output") {
        init {
            putValue(SMALL_ICON, ImageIcon(MainWindow::class.java.getResource("/images/clear_output.png")))
            putValue(SHORT_DESCRIPTION, "clears all output in output pane")
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, KeyEvent.META_DOWN_MASK))
        }

        override fun actionPerformed(e: ActionEvent?) {
            outputPane.document.remove(0, outputPane.document.length)
        }
    }

    inner class RemoveBindingAction() : AbstractAction("Remove Binding") {
        init {
            putValue(SMALL_ICON, ImageIcon(MainWindow::class.java.getResource("/images/remove_binding.png")))
            putValue(SHORT_DESCRIPTION, "removes selected binding from scope")
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.META_DOWN_MASK))
        }

        override fun actionPerformed(e: ActionEvent?) {
            val sel = scopeList.selectedValue ?: return
            context.removeBinding(sel)
            refresh()
        }
    }
}

class SmallIconButton(action: Action) : JButton(action) {
    init {
        border = BorderFactory.createEmptyBorder()
        hideActionText = true
    }
}
