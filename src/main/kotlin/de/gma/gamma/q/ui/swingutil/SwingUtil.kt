package de.gma.gamma.q.ui.swingutil

import javax.swing.UIManager
import javax.swing.plaf.nimbus.NimbusLookAndFeel


fun setLaf() {
    UIManager.setLookAndFeel(NimbusLookAndFeel())
}
