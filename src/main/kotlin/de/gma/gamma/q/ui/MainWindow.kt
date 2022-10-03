package de.gma.gamma.q.ui

import de.gma.gamma.parser.EvaluationException
import de.gma.gamma.q.Context
import de.gma.gamma.q.ui.swingutil.RasterLayout
import de.gma.gamma.q.ui.swingutil.WindowUtil
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

class MainWindow(private val context: Context) : JFrame() {
    private val scopeList = DefaultListModel<String>()
    private val watchList = DefaultListModel<String>()
    private val textPane = createEditor()
    private val editor = textPane.styledDocument
    private val errorLabel = JLabel()

    init {
        title = "Gamma Q – " + context.name
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
        scopeList.clear()
        scopeList.addAll(context.getBindings())
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
            add("1 3 12 -1", JScrollPane(JList(scopeList)))

            add("15 1", JLabel("Watch"))
            add("15 3 12 -1", JScrollPane(JList(watchList)))
        }

    private fun createEditorPanel() =
        JPanel(RasterLayout(50, 12)).apply {
            add("1 1 -1 -2", JScrollPane(textPane))

            add("1 -1 -1 1", errorLabel)
        }

    private fun createEditor() =
        JTextPane().apply {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), "execute")
            actionMap.put("execute", ExecuteAction())
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
                context.execute(editor.getText(0, editor.length))
                refresh()
                errorLabel.text = ""
            } catch (e: EvaluationException) {
                errorLabel.text = e.message
            } catch (e: Exception) {
                errorLabel.text = e.message
            }
        }
    }
}
