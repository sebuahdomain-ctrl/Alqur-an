package com.sabilu.alquran

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/** Ayat yang sedang disorot (kunci "surat:ayat"). Null = tidak ada sorotan. Hilang saat mushaf diketuk. */
object Sorot {
    @Volatile
    var kunci: String? = null
}

/**
 * Menggambar satu halaman Mushaf Madinah dari data per baris dan font QCF4.
 * Lebar glyph di font ini sudah menyesuaikan, jadi baris penuh cukup disambung lalu
 * diskalakan selebar halaman; baris pendek (awal surat, halaman 1 dan 2) diletakkan di tengah.
 */
class MushafPageView(context: Context) : View(context) {

    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.mushaf_text)
        isSubpixelText = true
        isLinearText = true
    }
    private val kuasTebal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        isFakeBoldText = true
        isSubpixelText = true
        isLinearText = true
    }
    private val kotakSorot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#BFE3C6")
    }
    private val garis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.mushaf_accent)
        strokeWidth = resources.displayMetrics.density * 1.2f
    }
    private val batas = Rect()
    private val bingkai = RectF()

    private var halaman: Halaman? = null
    private var faces: Map<String, Typeface> = emptyMap()
    private var lebarEm = FloatArray(0)

    init {
        // Latar halaman mushaf terang, terpisah dari tema gelap aplikasi.
        setBackgroundColor(context.getColor(R.color.mushaf_bg))
    }

    fun kosongkan() {
        halaman = null
        invalidate()
    }

    /** Mode terang (default) atau malam: hanya halaman mushaf yang berubah. */
    fun gayaMalam(on: Boolean) {
        kuas.color = context.getColor(if (on) R.color.text else R.color.mushaf_text)
        garis.color = context.getColor(if (on) R.color.accent else R.color.mushaf_accent)
        setBackgroundColor(context.getColor(if (on) R.color.page else R.color.mushaf_bg))
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
        // Tinggi blok teks = 1,75 x lebar (proporsi sedang), dibatasi tinggi layar.
        val tinggiBaris = minOf(tinggi / 15f, w * TINGGI_RASIO / 15f)
        val atas = pad + (tinggi - h.baris.size * tinggiBaris) / 2f
        val kunci = Sorot.kunci

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
                val awalX = pad + (w - lebar) / 2f
                val ada = kunci != null && b.kunci.contains(kunci)

                // Latar hijau di belakang kata ayat yang disorot.
                if (ada) {
                    val atasKotak = tengah - tinggiBaris * 0.45f
                    val bawahKotak = tengah + tinggiBaris * 0.45f
                    for (k in b.kunci.indices) {
                        if (b.kunci[k] != kunci) continue
                        val x0 = awalX + kuas.measureText(b.teks, 0, k)
                        val x1 = x0 + kuas.measureText(b.teks, k, k + 1)
                        canvas.drawRect(x0, atasKotak, x1, bawahKotak, kotakSorot)
                    }
                }
                canvas.drawText(b.teks, awalX, garisDasar, kuas)

                // Kata ayat yang disorot digambar tebal.
                if (ada) {
                    kuasTebal.typeface = tf
                    kuasTebal.textSize = ukuran
                    for (k in b.kunci.indices) {
                        if (b.kunci[k] != kunci) continue
                        val x0 = awalX + kuas.measureText(b.teks, 0, k)
                        canvas.drawText(b.teks, k, k + 1, x0, garisDasar, kuasTebal)
                    }
                }
            }
        }
    }

    private companion object {
        const val REF = 100f
        /** Lebar baris penuh di Mushaf Madinah, dalam satuan em font QCF4. */
        const val LEBAR_PENUH = 16.3f
        /** Tinggi blok 15 baris dibanding lebarnya. */
        const val TINGGI_RASIO = 1.75f
    }
}
