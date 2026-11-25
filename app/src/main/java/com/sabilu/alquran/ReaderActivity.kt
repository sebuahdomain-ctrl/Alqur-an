package com.sabilu.alquran

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import java.util.concurrent.RejectedExecutionException

class ReaderActivity : AppCompatActivity() {

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
        if (halaman < 1 || !FolderStore.isReadable(this)) {
            pesan.setText(R.string.data_tidak_ada)
            pesan.visibility = View.VISIBLE
            pager.visibility = View.GONE
            tampilkanBar(true)
            return
        }

        // Daftar isi folder dibaca di thread latar, baru halaman dibuka.
        pager.visibility = View.INVISIBLE
        pesan.setText(R.string.memuat)
        pesan.visibility = View.VISIBLE
        tampilkanBar(false)
        try {
            executor.execute {
                val siap = QuranFiles.siapkan(applicationContext)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (siap) {
                        mulai(pager, pesan, judul, sub, halaman)
                    } else {
                        pesan.setText(R.string.data_gagal)
                        pager.visibility = View.GONE
                        tampilkanBar(true)
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            // Layar sudah ditutup.
        }
    }

    private fun mulai(pager: ViewPager2, pesan: TextView, judul: TextView, sub: TextView, halaman: Int) {
        pesan.visibility = View.GONE
        pager.visibility = View.VISIBLE
        // Arah geser seperti mushaf: halaman 1 di kanan, geser ke kiri untuk lanjut.
        pager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        pager.adapter = PageAdapter()
        pager.offscreenPageLimit = 1
        // Halaman tujuan (mulai dari 1) diubah ke posisi 0, dan dijaga supaya tidak melewati halaman terakhir.
        val awal = (halaman - 1).coerceIn(0, Data.JUMLAH_HALAMAN - 1)
        pager.setCurrentItem(awal, false)
        perbarui(judul, sub, awal + 1)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                perbarui(judul, sub, position + 1)
            }
        })
    }

    /** Simbol penanda terisi kalau halaman yang sedang dibuka sudah ditandai. */
    private fun gantiIkonPenanda() {
        val ditandai = halamanSekarang >= 1 && PenandaStore.ada(this, halamanSekarang)
        tombolPenanda.setImageResource(if (ditandai) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
    }

    /** Isi judul dan keterangan bilah atas sesuai halaman mushaf yang sedang dibuka. */
    private fun perbarui(judul: TextView, sub: TextView, halaman: Int) {
        halamanSekarang = halaman
        gantiIkonPenanda()
        val (j, k) = Data.infoHalaman(halaman)
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
        super.onDestroy()
    }

    private inner class PageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = Data.JUMLAH_HALAMAN

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val tampilan = MushafPageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Ketuk halaman untuk memunculkan atau menyembunyikan bilah atas.
                setOnClickListener { tampilkanBar(!barTampil) }
            }
            return object : RecyclerView.ViewHolder(tampilan) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val tampilan = holder.itemView as MushafPageView
            val nomor = position + 1
            tampilan.tag = nomor
            tampilan.kosongkan()
            val app = applicationContext
            try {
                executor.execute {
                    val hal = QuranFiles.muatHalaman(app, nomor) ?: return@execute
                    val faces = HashMap<String, Typeface>()
                    for (b in hal.baris) {
                        if (faces.containsKey(b.font)) continue
                        QuranFiles.typeface(app, b.font)?.let { faces[b.font] = it }
                    }
                    tampilan.post { if (tampilan.tag == nomor) tampilan.tampilkan(hal, faces) }
                }
            } catch (e: RejectedExecutionException) {
                // Layar sedang ditutup.
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_PAGE = "page"
    }
}
