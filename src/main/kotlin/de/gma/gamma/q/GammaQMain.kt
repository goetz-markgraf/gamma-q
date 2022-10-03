package de.gma.gamma.q

import de.gma.gamma.q.ui.MainWindow
import de.gma.gamma.q.ui.swingutil.doSwingForMac
import de.gma.gamma.q.ui.swingutil.setLaf
import java.awt.EventQueue

fun main(args: Array<String>) {
    EventQueue.invokeLater {
        doSwingForMac()
        setLaf()
        MainWindow(Context("undefined")).open()
    }
}
