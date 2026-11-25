package com.sabilu.alquran

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.Executors

class ReaderActivity : AppCompatActivity() {

    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var bar: View
    private lateinit var kontrol: WindowInsetsControllerCompat
    private var barTampil = false
    private lateinit var tombolPenanda: ImageButton
    private var halamanSekarang = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Isi layar selalu sampai ke tepi, termasuk area poni/lubang kamera, supaya ukuran
        // layar tidak berubah waktu bilah ditampilkan atau disembunyikan (penyebab halaman bergetar).
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        setContentView(R.layout.activity_reader)

        // Layar tetap menyala selama membaca (juga diatur lewat keepScreenOn di layout).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Layar penuh: isi halaman sampai ke tepi layar, bilah sistem muncul sebentar kalau diusap dari tepi.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        kontrol = WindowCompat.getInsetsController(window, window.decorView)
        kontrol.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        bar = findViewById(R.id.bar)
        val padAtas = bar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(bar) { v, insets ->
            val atas = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            v.setPadding(v.paddingLeft, padAtas + atas, v.paddingRight, v.paddingBottom)
            insets
        }

        // Halaman diberi jarak tetap sebesar area poni, supaya tidak tertutup tapi juga tidak bergeser
        // saat bilah muncul atau hilang. Di HP tanpa poni jaraknya 0, jadi benar-benar penuh.
        val pagerView = findViewById<ViewPager2>(R.id.pager)
        ViewCompat.setOnApplyWindowInsetsListener(pagerView) { v, insets ->
            val c = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val lp = v.layoutParams as ViewGroup.MarginLayoutParams
            if (lp.leftMargin != c.left || lp.topMargin != c.top ||
                lp.rightMargin != c.right || lp.bottomMargin != c.bottom
            ) {
                lp.setMargins(c.left, c.top, c.right, c.bottom)
                v.layoutParams = lp
            }
            insets
        }

        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE)
        findViewById<View>(R.id.back).setOnClickListener { finish() }

        tombolPenanda = findViewById(R.id.penanda)
        tombolPenanda.setOnClickListener {
            if (halamanSekarang < 1) return@setOnClickListener
            val ditandai = PenandaStore.ubah(this, halamanSekarang)
            gantiIkonPenanda()
            Toast.makeText(
                this,
                if (ditandai) R.string.penanda_ditambah else R.string.penanda_dihapus,
                Toast.LENGTH_SHORT
            ).show()
        }

        val pesan = findViewById<TextView>(R.id.pesan)
        val pager = findViewById<ViewPager2>(R.id.pager)
        val judul = findViewById<TextView>(R.id.title)
        val sub = findViewById<TextView>(R.id.sub)

        val halaman = intent.getIntExtra(EXTRA_PAGE, -1)
        val uri = PdfStore.get(this)
        if (uri == null || halaman < 1) {
            pesan.setText(R.string.pdf_tidak_ada)
            pesan.visibility = View.VISIBLE
            tampilkanBar(true)
            return
        }

        try {
            val d = contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("fd null")
            fd = d
            val r = PdfRenderer(d)
            renderer = r
            if (r.pageCount == 0) throw IllegalStateException("kosong")

            // Arah geser seperti mushaf: halaman 1 di kanan, geser ke kiri untuk lanjut.
            pager.layoutDirection = View.LAYOUT_DIRECTION_RTL
            pager.adapter = PageAdapter(r)
            pager.offscreenPageLimit = 1
            // Halaman tujuan (mulai dari 1) diubah ke posisi 0, dan dijaga supaya tidak melewati halaman terakhir.
            val awal = (halaman - 1).coerceIn(0, r.pageCount - 1)
            pager.setCurrentItem(awal, false)
            perbarui(judul, sub, awal + 1)
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    perbarui(judul, sub, position + 1)
                }
            })
        } catch (e: Exception) {
            pesan.setText(R.string.pdf_gagal)
            pesan.visibility = View.VISIBLE
            pager.visibility = View.GONE
            tampilkanBar(true)
            return
        }

        tampilkanBar(false)
    }

    /** Simbol penanda terisi kalau halaman yang sedang dibuka sudah ditandai. */
    private fun gantiIkonPenanda() {
        val ditandai = halamanSekarang >= 1 && PenandaStore.ada(this, halamanSekarang)
        tombolPenanda.setImageResource(if (ditandai) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
    }

    /** Isi judul dan keterangan bilah atas sesuai halaman PDF yang sedang dibuka. */
    private fun perbarui(judul: TextView, sub: TextView, halamanPdf: Int) {
        halamanSekarang = halamanPdf
        gantiIkonPenanda()
        val (j, k) = Data.infoHalaman(halamanPdf)
        judul.text = j
        sub.text = k
        sub.visibility = if (k == null) View.GONE else View.VISIBLE
    }

    /** Tampilkan atau sembunyikan bilah atas bersama bilah sistem (status dan navigasi). */
    private fun tampilkanBar(tampil: Boolean) {
        barTampil = tampil
        bar.visibility = if (tampil) View.VISIBLE else View.GONE
        if (tampil) kontrol.show(WindowInsetsCompat.Type.systemBars())
        else kontrol.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        executor.shutdownNow()
        // Tunggu sebentar supaya render yang sedang jalan selesai sebelum renderer ditutup.
        renderer?.let { r ->
            synchronized(r) { runCatching { r.close() } }
        }
        runCatching { fd?.close() }
        super.onDestroy()
    }

    private inner class PageAdapter(private val r: PdfRenderer) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = r.pageCount

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val img = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                // Ketuk halaman untuk memunculkan atau menyembunyikan bilah atas.
                setOnClickListener { tampilkanBar(!barTampil) }
            }
            return object : RecyclerView.ViewHolder(img) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val img = holder.itemView as ImageView
            img.setImageBitmap(null)
            img.tag = position
            img.post {
                val w = img.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
                val h = img.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
                if (executor.isShutdown) return@post
                executor.execute {
                    val bmp = render(position, w, h) ?: return@execute
                    img.post { if (img.tag == position) img.setImageBitmap(bmp) }
                }
            }
        }

        private fun render(index: Int, w: Int, h: Int): Bitmap? = synchronized(r) {
            try {
                val page = r.openPage(index)
                try {
                    val skala = minOf(w.toFloat() / page.width, h.toFloat() / page.height)
                    val bw = (page.width * skala).toInt().coerceAtLeast(1)
                    val bh = (page.height * skala).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                } finally {
                    page.close()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_PAGE = "page"
    }
}
