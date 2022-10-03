package de.gma.gamma.q.ui.swingutil

import java.awt.GridBagConstraints
import java.awt.Insets

/**
 * Einfaches GridBagContraints-Objekt.
 *
 *
 * ** Benutzung**
 *
 *
 * Für ein komplettes GridbagLayout kann (soll) immer genau eine Instanz von GridBagConstraints2 verwendet werden.
 * Die Standardwerte sind so eingestellt, dass sie für die meisten Controls sinnvoll sind.
 *
 *
 * Jedes neue Control wird stets rechts neben das letzte gestellt. Soll eine neue Zeile beginnen werden, ruft man `newLine()` auf.
 *
 *
 * Über folgende Methoden kann eine spezielle Version des GBC2 für genau ein Control erzeugt werden.
 * ``
 *  * pad(x, y);
 *  * remainder();
 *  * columnRemainder()
 *  * weightx(d);
 *  * weighty(d);
 *  * width(i);
 *  * height(i);
 *  * fill(f);
 *  * anchor(a);
 *
 * Dabei wird das ursprüngliche GBC2 nicht manipuliert, sondern eine veränderte Kopie zurückgegeben. Die Methoden können auch hintereinandergehängt werden:
 *
 *
 * ` gbc2.weightx(1.0).width(2);`
 *
 * @author Dragon
 */
class GridBagConstraints2(top: Int = 5, left: Int = 10, bottom: Int = 5, right: Int = 10) :
    GridBagConstraints() {
    /**
     * Ist diese Instanz von GBC schon kopiert worden? Wenn ja, dann muss ich es nicht neu kopieren.
     */
    private var cloned = false

    init {
        fill = HORIZONTAL
        anchor = BASELINE_LEADING
        gridx = RELATIVE
        gridy = 0
        weightx = 0.0
        weighty = 0.0
        insets = Insets(top, left, bottom, right)
    }

    /**
     * @param anchor
     * @return
     */
    fun anchor(anchor: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.anchor = anchor
        return cl
    }

    fun columnRemainder(): GridBagConstraints2 {
        val cl = doClone()
        cl.gridheight = REMAINDER
        return cl
    }

    /**
     * Nur dieses eine Component hat eine andere fill-Einstellung
     *
     * @param i
     */
    fun fill(fill: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.fill = fill
        return cl
    }

    /**
     * Setzt die X-Position. Damit kann man Spalten überspringen
     *
     * @param x
     * @return
     */
    fun gridx(x: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.gridx = x
        return cl
    }

    /**
     * Setzt die Y-Position. Damit kann man Zeilen überspringen
     *
     * @param y
     * @return
     */
    fun gridy(y: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.gridy = y
        return cl
    }

    /**
     * Gibt an, über wie viele Zeilen sich dieses Control erstrecken soll.
     *
     * @param i
     */
    fun height(i: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.gridheight = i
        return cl
    }

    /**
     * Ab hier soll eine neue Zeile beginnen. Dieser Befehl ist der einzige, der das Originalobejkt verändert.
     *
     * @return
     */
    fun newline(): GridBagConstraints2 {
        gridy++
        return this
    }

    /**
     * Vergrößert das Control in x oder y-Richtung um die angegebene Anzahl von Pixeln
     *
     * @param i
     * @param j
     */
    fun pad(padx: Int, pady: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.ipadx = padx
        cl.ipady = pady
        return cl
    }

    /**
     * Das Control soll den ganzen Rest der Zeile einnehmen
     *
     * @return
     */
    fun remainder(): GridBagConstraints2 {
        val cl = doClone()
        cl.gridx = REMAINDER
        return cl
    }

    /**
     * Gibt die horizontale Gewichtung beim Resize (Vergrößerung oder nicht) für dieses Control an.
     *
     * @param wx
     * @return
     */
    fun weightx(wx: Double): GridBagConstraints2 {
        val cl = doClone()
        cl.weightx = wx
        return cl
    }

    /**
     * Gibt die verticale Gewichtung beim Resize (Vergrößerung oder nicht) für dieses Control an.
     *
     * @param wy
     * @return
     */
    fun weighty(wy: Double): GridBagConstraints2 {
        val cl = doClone()
        cl.weighty = wy
        cl.fill = BOTH
        return cl
    }

    /**
     * Gibt an, über wie viele Spalten sich dieses Control erstrecken soll.
     *
     * @param i
     */
    fun width(i: Int): GridBagConstraints2 {
        val cl = doClone()
        cl.gridwidth = i
        return cl
    }

    /**
     * Erzeugt eine Kopie, wenn wir nicht schon eine Kopie haben
     * @return Kopie des GBC
     */
    private fun doClone(): GridBagConstraints2 {
        if (cloned) return this
        val cl = clone() as GridBagConstraints2
        cl.cloned = true
        return cl
    }

    companion object {
        private const val serialVersionUID = 4648106248941108334L
    }
}
