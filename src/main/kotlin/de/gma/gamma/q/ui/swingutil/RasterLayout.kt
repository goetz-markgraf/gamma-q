package de.gma.gamma.q.ui.swingutil

import java.awt.*
import javax.swing.JButton
import javax.swing.JComponent

/**
 * @author Dragon
 */
class RasterLayout(var minimumSize: Dimension = Dimension(100, 100)) : LayoutManager {
    private val _components: MutableMap<Component, RasterLayoutConstraints> = HashMap()

    constructor(x: Int, y: Int) : this(Dimension(x, y)) {}

    /**
     * @see java.awt.LayoutManager.addLayoutComponent
     */
    override fun addLayoutComponent(constraints: String, comp: Component) {
        _components[comp] = RasterLayoutConstraints(constraints)
        if (comp is JButton) {
            comp.margin = Insets(1, 1, 1, 1)
        }
    }

    private fun getRaster(parent: Container): Dimension {
        val fm = parent.getFontMetrics(parent.font)
        val height = fm.ascent + fm.descent
        val width = fm.stringWidth("mb") - fm.stringWidth("b")
        return Dimension(width, height)
    }

    /**
     * @see java.awt.LayoutManager.layoutContainer
     */
    override fun layoutContainer(parent: Container) {
        val raster = getRaster(parent)
        val parentSize = parent.size
        val parentInsets = parent.insets
        parentSize.width -= parentInsets.left + parentInsets.right
        parentSize.height -= parentInsets.top + parentInsets.bottom
        for (i in 0 until parent.componentCount) {
            val c = parent.getComponent(i)
            val cons = _components[c]
            if (cons != null) {
                val rect = cons.getRectangle(c, raster, parentSize)
                if (rect != null) {
                    rect.x += parentInsets.left
                    rect.y += parentInsets.top
                    if (c is JComponent) {
                        val insets = c.insets
                        rect.x -= insets.left
                        rect.y -= insets.top
                        rect.width += insets.left + insets.right
                        rect.height += insets.top + insets.bottom
                    }
                    c.bounds = rect
                }
            }
        }
    }

    /**
     * @see java.awt.LayoutManager.minimumLayoutSize
     */
    override fun minimumLayoutSize(parent: Container): Dimension {
        return preferredLayoutSize(parent)
    }

    /**
     * @see java.awt.LayoutManager.preferredLayoutSize
     */
    override fun preferredLayoutSize(parent: Container): Dimension {
        val raster = getRaster(parent)
        return Dimension(
            minimumSize.width * raster.width,
            minimumSize.height * raster.height
        )
    }

    /**
     * @see java.awt.LayoutManager.removeLayoutComponent
     */
    override fun removeLayoutComponent(comp: Component) {
        _components.remove(comp)
    }

    private inner class RasterLayoutConstraints(constraints: String) {
        private var xType = TYPE_UNKNOWN
        private var yType = TYPE_UNKNOWN
        private var xPlus = 0
        private var xMinus = 0
        private var yPlus = 0
        private var yMinus = 0
        private var xSize = 0
        private var ySize = 0

        init {
            convertToConstraints(constraints)
        }

        /**
         * Method getRectangle.
         *
         * @param raster
         * @param dimension
         * @return Rectangle
         */
        fun getRectangle(
            comp: Component,
            raster: Dimension,
            parentDimension: Dimension
        ): Rectangle? {
            if (xType == TYPE_UNKNOWN || yType == TYPE_UNKNOWN) return null
            val size = comp.preferredSize
            val x: Int
            val y: Int
            val w: Int
            val h: Int
            when (xType) {
                TYPE_PLUS_ONLY -> {
                    x = raster.width * xPlus
                    w = size.width
                }

                TYPE_MINUS_ONLY -> {
                    w = size.width
                    x = parentDimension.width - w - raster.width * xMinus
                }

                TYPE_PLUS_MINUS -> {
                    x = raster.width * xPlus
                    w = parentDimension.width - raster.width * xMinus - x
                }

                TYPE_PLUS_SIZE -> {
                    x = raster.width * xPlus
                    w = raster.width * xSize
                }

                TYPE_MINUS_SIZE -> {
                    w = raster.width * xSize
                    x = parentDimension.width - w - raster.width * xMinus
                }

                else -> return null
            }
            when (yType) {
                TYPE_PLUS_ONLY -> {
                    y = raster.height * yPlus
                    h = size.height
                }

                TYPE_MINUS_ONLY -> {
                    h = size.height
                    y = parentDimension.height - h - raster.height * yMinus
                }

                TYPE_PLUS_MINUS -> {
                    y = raster.height * yPlus
                    h = parentDimension.height - raster.height * yMinus - y
                }

                TYPE_PLUS_SIZE -> {
                    y = raster.height * yPlus
                    h = raster.height * ySize
                }

                TYPE_MINUS_SIZE -> {
                    h = raster.height * ySize
                    y = parentDimension.height - h - raster.height * yMinus
                }

                else -> return null
            }
            return Rectangle(x, y, w, h)
        }

        /**
         * Method convertToConstraints.
         *
         * @param name
         * @return Object
         */
        fun convertToConstraints(constraints: String) {
            val elements: MutableList<String> = constraints.split(' ').toMutableList()

            // Anzahl von Elementen
            if (elements.size == 2) {
                elements.add("0")
                elements.add("0")
            }
            if (elements.size != 4) return

            // Prüfe Gültigkeit der ersten Werte
            val x1: Int
            val y1: Int
            val x2: Int
            val y2: Int
            try {
                x1 = elements[0].toInt()
                y1 = elements[1].toInt()
                x2 = elements[2].toInt()
                y2 = elements[3].toInt()
            } catch (e: NumberFormatException) {
                return
            }

            // Check TypeX
            if (x1 >= 0 && x2 == 0) {
                xType = TYPE_PLUS_ONLY
                xPlus = x1
            } else if (x1 >= 0 && x2 < 0) {
                xType = TYPE_PLUS_MINUS
                xPlus = x1
                xMinus = -x2
            } else if (x1 >= 0) {
                xType = TYPE_PLUS_SIZE
                xPlus = x1
                xSize = x2
            } else if (x1 < 0 && x2 == 0) {
                xType = TYPE_MINUS_ONLY
                xMinus = -x1
            } else if (x1 < 0 && x2 > 0) {
                xType = TYPE_MINUS_SIZE
                xMinus = -x1
                xSize = x2
            } else return

            // Check TypeY
            if (y1 >= 0 && y2 == 0) {
                yType = TYPE_PLUS_ONLY
                yPlus = y1
            } else if (y1 >= 0 && y2 < 0) {
                yType = TYPE_PLUS_MINUS
                yPlus = y1
                yMinus = -y2
            } else if (y1 >= 0) {
                yType = TYPE_PLUS_SIZE
                yPlus = y1
                ySize = y2
            } else if (y1 < 0 && y2 == 0) {
                yType = TYPE_MINUS_ONLY
                yMinus = -y1
            } else if (y1 < 0 && y2 > 0) {
                yType = TYPE_MINUS_SIZE
                yMinus = -y1
                ySize = y2
            }
        }
    }

    companion object {
        private const val TYPE_UNKNOWN = 0
        private const val TYPE_PLUS_ONLY = 1
        private const val TYPE_MINUS_ONLY = 2
        private const val TYPE_PLUS_SIZE = 3
        private const val TYPE_MINUS_SIZE = 4
        private const val TYPE_PLUS_MINUS = 5
    }
}
