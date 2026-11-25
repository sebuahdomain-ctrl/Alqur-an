package com.sabilu.alquran

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * Animasi buka-tutup daftar surat di dalam juz, seperti bendera tergulung yang ditarik ke bawah.
 * Baris surat tetap di tempatnya dan hanya dipotong dari atas ke bawah (clipBounds), baris di bawahnya
 * ikut bergeser, dan gulungan kain di tepi bawah makin tipis saat ditarik (makin tebal saat digulung).
 */
class Gulungan(private val rv: RecyclerView, private val adapter: EntryAdapter) {

    private val density = rv.resources.displayMetrics.density
    private val barisPx = 56 * density

    private var awal = 0      // posisi baris surat pertama di dalam juz
    private var jumlah = 0    // banyak baris surat
    private var p = 0f        // 0 = tergulung penuh, 1 = terbuka penuh
    private var tujuan = 0f
    private var geser = 0f   // seberapa jauh isi daftar harus turun saat tutup, kalau daftar sudah di ujung bawah
    private var aktif = false
    private var animator: ValueAnimator? = null

    /** Nomor juz yang sedang bergerak, atau -1. */
    var juz = -1
        private set

    private val kain = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(0xFF4A514C.toInt(), 0xFF2C322E.toInt(), 0xFF171A18.toInt(), 0xFF0C0E0D.toInt())
    )
    private val bayangan = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(0x88000000.toInt(), 0x00000000)
    )

    init {
        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                if (!aktif || p <= 0.001f || p >= 0.999f) return
                val baris = parent.layoutManager?.findViewByPosition(awal - 1) ?: return
                val h = jumlah * barisPx * p
                val r = minOf(h, (20f * (1f - p) + 4f) * density)
                val atas = baris.bottom + baris.translationY + h - r
                val bawah = atas + r
                bayangan.setBounds(0, bawah.toInt(), parent.width, (bawah + 8 * density).toInt())
                bayangan.draw(c)
                kain.cornerRadius = r / 2f
                kain.setBounds(0, atas.toInt(), parent.width, bawah.toInt())
                kain.draw(c)
            }
        })
        // Baris yang baru muncul di layar saat animasi berjalan langsung ikut aturan potong.
        rv.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                if (aktif) terapkanKe(view)
            }

            override fun onChildViewDetachedFromWindow(view: View) {}
        })
    }

    fun berjalan() = aktif

    /** Buka juz: sisipkan surat-suratnya, lalu tarik kain ke bawah. */
    fun buka(no: Int, posJuz: Int, anak: List<Entry>) {
        juz = no
        awal = posJuz + 1
        jumlah = anak.size
        p = 0f
        tujuan = 1f
        geser = 0f
        aktif = true
        adapter.sisipkan(posJuz, anak)
        // Tunggu layout selesai tapi sebelum digambar, supaya tidak ada satu frame penuh yang berkedip.
        rv.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                rv.viewTreeObserver.removeOnPreDrawListener(this)
                terapkan()
                mulai(1f)
                return true
            }
        })
    }

    /** Tutup juz: gulung kain ke atas, lalu hapus baris suratnya. */
    fun tutup(no: Int, posJuz: Int, banyak: Int) {
        juz = no
        awal = posJuz + 1
        jumlah = banyak
        p = 1f
        aktif = true
        // Kalau daftar sudah di ujung bawah, setelah baris dihapus isi daftar akan turun mengisi ruang kosong.
        // Hitung jaraknya dari awal, lalu turunkan sedikit demi sedikit selama animasi, supaya tidak melompat di akhir.
        geser = 0f
        val lm = rv.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        val terakhir = lm?.findViewByPosition(adapter.itemCount - 1)
        if (terakhir != null) {
            val sisa = rv.height - rv.paddingBottom - (terakhir.bottom - jumlah * barisPx)
            if (sisa > 0f) geser = minOf(sisa, rv.computeVerticalScrollOffset().toFloat())
        }
        mulai(0f)
    }

    /** Diketuk lagi saat masih bergerak: langsung berbalik arah dari posisi sekarang. */
    fun balik(buka: Boolean) = mulai(if (buka) 1f else 0f)

    /** Hentikan animasi dan lompat ke kondisi akhirnya. */
    fun selesaikan() {
        if (!aktif) return
        animator?.apply { removeAllListeners(); cancel() }
        selesai(tujuan)
    }

    private fun mulai(ke: Float) {
        animator?.apply { removeAllListeners(); cancel() }
        tujuan = ke
        val dari = p
        if (dari == ke) {
            selesai(ke)
            return
        }
        animator = ValueAnimator.ofFloat(dari, ke).apply {
            duration = maxOf(80L, (340L * abs(ke - dari)).toLong())
            interpolator = if (ke > dari) DecelerateInterpolator(1.5f) else PathInterpolator(0.65f, 0f, 0.35f, 1f)
            addUpdateListener {
                p = it.animatedValue as Float
                terapkan()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) = selesai(ke)
            })
            start()
        }
    }

    private fun selesai(ke: Float) {
        animator = null
        p = ke
        geser = 0f
        for (i in 0 until rv.childCount) {
            val v = rv.getChildAt(i)
            v.clipBounds = null
            v.translationY = 0f
        }
        if (ke == 0f) adapter.hapus(awal, jumlah)
        aktif = false
        juz = -1
        rv.invalidate()
    }

    private fun terapkan() {
        for (i in 0 until rv.childCount) terapkanKe(rv.getChildAt(i))
        rv.invalidate()
    }

    private fun terapkanKe(v: View) {
        val pos = rv.getChildAdapterPosition(v)
        if (pos == RecyclerView.NO_POSITION) return
        val tinggi = jumlah * barisPx
        val tampil = tinggi * p
        val turun = geser * (1f - p)
        when {
            pos in awal until awal + jumlah -> {
                // Baris surat: hanya bagian yang sudah "ditarik" yang terlihat.
                val atas = (pos - awal) * barisPx
                val bagian = (tampil - atas).coerceIn(0f, barisPx)
                v.translationY = turun
                v.clipBounds = Rect(0, 0, v.width, bagian.toInt())
            }
            pos >= awal + jumlah -> {
                // Baris di bawah juz: naik mengikuti kain yang belum ditarik.
                v.clipBounds = null
                v.translationY = -(tinggi - tampil) + turun
            }
            else -> {
                v.clipBounds = null
                v.translationY = turun
            }
        }
    }
}
