package com.sabilu.alquran

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.Executors

class ReaderActivity : AppCompatActivity() {

    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        findViewById<TextView>(R.id.title).text = intent.getStringExtra(EXTRA_TITLE)
        findViewById<View>(R.id.back).setOnClickListener { finish() }

        val pesan = findViewById<TextView>(R.id.pesan)
        val pager = findViewById<ViewPager2>(R.id.pager)
        val nomor = findViewById<TextView>(R.id.halaman)

        val uri = intent.getStringExtra(EXTRA_URI)?.let { Uri.parse(it) }
        if (uri == null) {
            pesan.setText(R.string.pdf_tidak_ada)
            pesan.visibility = View.VISIBLE
            return
        }

        try {
            val d = contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("fd null")
            fd = d
            val r = PdfRenderer(d)
            renderer = r
            if (r.pageCount == 0) throw IllegalStateException("kosong")

            pager.adapter = PageAdapter(r)
            pager.offscreenPageLimit = 1
            nomor.text = "1 / ${r.pageCount}"
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    nomor.text = "${position + 1} / ${r.pageCount}"
                }
            })
        } catch (e: Exception) {
            pesan.setText(R.string.pdf_gagal)
            pesan.visibility = View.VISIBLE
            pager.visibility = View.GONE
        }
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
        const val EXTRA_URI = "uri"
    }
}
