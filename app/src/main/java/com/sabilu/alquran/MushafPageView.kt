package com.sabilu.alquran

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * Menggambar satu halaman Mushaf Madinah dari data per baris dan font QCF4.
 * Lebar glyph di font ini sudah menyesuaikan, jadi baris penuh cukup disambung lalu
 * diskalakan selebar halaman; baris pendek (awal surat, halaman 1 dan 2) diletakkan di tengah.
 */
class MushafPageView(context: Context) : View(context) {

    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text)
        isSubpixelText = true
        isLinearText = true
    }
    private val garis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.accent)
        strokeWidth = resources.displayMetrics.density * 1.2f
    }
    private val batas = Rect()
    private val bingkai = RectF()

    private var halaman: Halaman? = null
    private var faces: Map<String, Typeface> = emptyMap()
    private var lebarEm = FloatArray(0)

    fun kosongkan() {
        halaman = null
        invalidate()
    }

    fun tampilkan(h: Halaman, f: Map<String, Typeface>) {
        halaman = h
        faces = f
        // Lebar tiap baris dalam satuan em (diukur sekali, lalu diskalakan saat menggambar).
        lebarEm = FloatArray(h.baris.size) { i ->
            val b = h.baris[i]
            val tf = f[b.font]
            if (tf == null) {
                0f
            } else {
                kuas.typeface = tf
                kuas.textSize = REF
                kuas.measureText(b.teks) / REF
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val h = halaman ?: return
        val pad = resources.displayMetrics.density * 10f
        val w = width - 2f * pad
        val tinggi = height - 2f * pad
        if (w <= 0f || tinggi <= 0f) return

        val dasar = w / LEBAR_PENUH
        val tinggiBaris = minOf(tinggi / 15f, dasar * 2.4f)
        val atas = pad + (tinggi - h.baris.size * tinggiBaris) / 2f

        for (i in h.baris.indices) {
            val b = h.baris[i]
            val tf = faces[b.font] ?: continue
            val natural = lebarEm[i]
            if (natural <= 0f) continue
            kuas.typeface = tf
            val tengah = atas + tinggiBaris * (i + 0.5f)

            if (b.jenis == "surah_header") {
                kuas.textSize = dasar * 1.3f
                val lebar = natural * kuas.textSize
                kuas.getTextBounds(b.teks, 0, b.teks.length, batas)
                val garisDasar = tengah - (batas.top + batas.bottom) / 2f
                bingkai.set(pad, tengah - tinggiBaris * 0.4f, pad + w, tengah + tinggiBaris * 0.4f)
                canvas.drawRoundRect(bingkai, tinggiBaris * 0.15f, tinggiBaris * 0.15f, garis)
                canvas.drawText(b.teks, pad + (w - lebar) / 2f, garisDasar, kuas)
            } else {
                // Baris penuh diskalakan sedikit supaya pas selebar halaman; baris pendek tetap
                // berukuran dasar dan diletakkan di tengah.
                var ukuran = dasar
                if (natural * dasar >= w * 0.93f) ukuran = w / natural
                kuas.textSize = ukuran
                val lebar = natural * ukuran
                val fm = kuas.fontMetrics
                val garisDasar = tengah - (fm.ascent + fm.descent) / 2f
                canvas.drawText(b.teks, pad + (w - lebar) / 2f, garisDasar, kuas)
            }
        }
    }

    private companion object {
        const val REF = 100f
        /** Lebar baris penuh di Mushaf Madinah, dalam satuan em font QCF4. */
        const val LEBAR_PENUH = 16.3f
    }
}
