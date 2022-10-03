package de.gma.gamma.q.ui.swingutil

import java.util.*


/**
 * Liefert zurück, ob wir auf einem Mac arbeiten.
 *
 * @return
 */
fun isMac() = System.getProperty("os.name").lowercase(Locale.getDefault()).indexOf("mac") >= 0

/**
 * Hilft, die wichtigsten Dinge auf dem Mac sauber zu erstellen.
 *
 * Um den Namen einer Anwendung zu setzen, muss die VM den Parameter den folgenden Parameter erhalten
 *
 * -Xdock:name="Hallo Welt"
 *
 * @author dragon
 *
 */
fun doSwingForMac() {
    if (!isMac()) return

    // Damit werden auch JMenuBar-Objekte auf der OS X Menubar abgelegt
    System.setProperty("apple.laf.useScreenMenuBar", "true")

    // Damit ein Quit nicht sofort alles beendet, sondern sauber die Fenster zumacht.
//    val app = Application.getApplication()
//    app.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS)
}
