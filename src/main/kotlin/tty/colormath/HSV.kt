package klisp.tty.colormath

import kotlin.math.floor
import kotlin.math.max

data class HSV(val h: Int, val s: Int, val v: Int) : ConvertibleColor {
    init {
        require(h in 0..360) { "h must be in range [0, 360]" }
        require(s in 0..100) { "s must be in range [0, 100]" }
        require(v in 0..100) { "v must be in range [0, 100]" }
    }

    override fun toRGB(): RGB {
        val h = h.toDouble() / 60
        val s = s.toDouble() / 100
        var v = v.toDouble() / 100
        val hi = floor(h) % 6

        val f = h - floor(h)
        val p = 255 * v * (1 - s)
        val q = 255 * v * (1 - (s * f))
        val t = 255 * v * (1 - (s * (1 - f)))
        v *= 255

        val (r, g, b) = when (hi.toInt()) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return RGB(r.roundToInt(), g.roundToInt(), b.roundToInt())
    }

    override fun toHSL(): HSL {
        val h = h.toDouble()
        val s = s.toDouble() / 100
        val v = v.toDouble() / 100
        val vmin = max(v, 0.01)

        val l = ((2 - s) * v) / 2
        val lmin = (2 - s) * vmin
        val sl = (s * vmin) / (if (lmin <= 1) lmin else 2 - lmin)

        return HSL(h.roundToInt(), (sl * 100).roundToInt(), (l * 100).roundToInt())
    }

    override fun toHSV() = this
}
