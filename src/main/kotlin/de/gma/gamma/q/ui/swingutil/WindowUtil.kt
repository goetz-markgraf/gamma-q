/*
 * Created on 01.07.2003
 */
package de.gma.gamma.q.ui.swingutil

import java.awt.*
import java.awt.event.*

/**
 * <h3>Function</h3>
 * <h3>Usage</h3>
 *
 * @author Goetz Markgraf
 */
object WindowUtil {
    private const val MAX = 300
    private val minSizeListener = MinSizeListener()
    private var pos = 0
    private const val STEP = 30

    fun centerWindow(toCenter: Window) {
        toCenter.location = getCenter(toCenter.size)
    }

    fun centerWindowFirstThird(toPlace: Window) {
        toPlace.location = getCenterFirstThird(toPlace.size)
    }

    /**
     * Platziert ein Fenster mittig über ein anderes
     *
     */
    fun centerWindowOver(toPlace: Window, placeOver: Window) {
        val bounds = placeOver.bounds

        // Wo muss der Punkt �ber dem anderen Fenster sein?
        val offset = getCenterOver(toPlace.size, bounds.size)
        // Addiere noch die Startposition des anderen Fensters hinzu
        offset.translate(bounds.getX().toInt(), bounds.getY().toInt())
        toPlace.location = offset
    }

    /**
     * @param win
     */
    fun ensureMinimumSize(win: Window) {
        win.addComponentListener(minSizeListener)
    }

    /**
     * Stellt sicher, dass das Fenster immer die angegebenen Bounds hat.
     *
     * @param f
     */
    fun freezeWindow(f: Window) {
        class FreezeListener(private val _freezeBounds: Rectangle) : WindowAdapter(), ComponentListener {
            override fun componentHidden(e: ComponentEvent) {
                resetBounds(e)
            }

            override fun componentMoved(e: ComponentEvent) {
                resetBounds(e)
            }

            override fun componentResized(e: ComponentEvent) {
                resetBounds(e)
            }

            override fun componentShown(e: ComponentEvent) {
                resetBounds(e)
            }

            private fun resetBounds(e: ComponentEvent) {
                val comp = e.source as Component
                if (comp.bounds != _freezeBounds) comp.bounds = _freezeBounds
            }

            override fun windowIconified(e: WindowEvent) {
                val window = e.window
                if (window is Frame) window.extendedState = Frame.NORMAL
            }
        }

        // Erstelle einen neuen Listener und setze ihn zum Einfrieren
        // der Bounds
        val freeze = FreezeListener(f.bounds)
        f.addComponentListener(freeze)
        f.addWindowListener(freeze)
    }

    fun getCenter(toCenter: Dimension): Point {
        val dim = Toolkit.getDefaultToolkit().screenSize
        return getCenterOver(toCenter, dim)
    }

    /**
     * Liefert einen Punkt zur�ck, der ein Rechteck �ber einem anderen Rechteck zentriert
     * @param toCenter
     * @param dimOver
     * @return
     */
    private fun getCenterOver(toCenter: Dimension, dimOver: Dimension): Point {
        val x = (dimOver.width - toCenter.width) / 2
        val y = (dimOver.height - toCenter.height) / 2
        return Point(x, y)
    }

    fun getCenterFirstThird(toPlace: Dimension): Point {
        val dim = Toolkit.getDefaultToolkit().screenSize
        val x = (dim.width - toPlace.width) / 2
        val y = (dim.height - toPlace.height) / 2 * 2 / 3
        return Point(x, y)
    }

    private val defaultLocation: Point
        get() {
            pos += 30
            if (pos > 200) pos = 30
            return Point(pos, pos)
        }

    /**
     * @param component
     */
    fun getFrame(component: Component?): Frame? {
        val ret: Frame? = null
        var c = component
        while (c != null) {
            if (c is Frame) return c
            c = c.parent
        }
        return null
    }

    fun maximizeWindow(aWindow: Window) {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration
        )
        aWindow.setSize(
            screenSize.width - screenInsets.left - screenInsets.right,
            screenSize.height - screenInsets.top - screenInsets.bottom
        )
        if (aWindow is Frame) {
            aWindow.extendedState = Frame.MAXIMIZED_BOTH
        }
    }

    fun placeDefault(toPlace: Window) {
        toPlace.location = defaultLocation
    }

    fun topMost(win: Window) {
        win.isAlwaysOnTop = true
    }

    internal class MinSizeListener : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            val size = e.component.size
            val minSize = e.component.preferredSize
            size.width = Math.max(size.width, minSize.width)
            size.height = Math.max(size.height, minSize.height)
            e.component.size = size
        }
    }
}
