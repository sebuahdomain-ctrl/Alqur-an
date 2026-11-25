package com.sabilu.alquran

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    private val tabIds = listOf(R.id.tab_surat, R.id.tab_juz, R.id.tab_penanda)

    private lateinit var pager: ViewPager2
    private lateinit var indicator: View
    private var lebarTab = 0

    /** Juz yang sedang dibuka daftar suratnya. */
    private val juzTerbuka = HashSet<Int>()

    /** Adapter dan daftar tab Juz, dipakai untuk membuka atau menutup juz tanpa membangun ulang layar. */
    private var juzAdapter: EntryAdapter? = null
    private var juzRv: RecyclerView? = null

    private fun daftar(posisi: Int): List<Entry> = when (posisi) {
        0 -> Data.surat
        1 -> juzTampil()
        else -> PenandaStore.semua(this).map { Data.entri(it) }
    }

    /** Daftar juz, dengan surat-surat di dalam juz yang sedang dibuka di bawahnya. */
    private fun juzTampil(): List<Entry> = Data.juz.flatMap { j ->
        val no = j.juz ?: return@flatMap listOf(j)
        if (juzTerbuka.contains(no)) listOf(j) + Data.anakJuz(no) else listOf(j)
    }

    private fun togglePanah(item: Entry) {
        val no = item.juz ?: return
        val adapter = juzAdapter ?: return
        val rv = juzRv ?: return
        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        val pos = adapter.posisiJuz(no)
        if (pos < 0) return

        val buka = !juzTerbuka.contains(no)
        if (buka) {
            juzTerbuka.add(no)
            val anak = Data.anakJuz(no)
            adapter.sisipkan(pos, anak)
            // Surat di dalam juz muncul dari atas ke bawah, satu per satu.
            rv.post { munculkan(lm, pos + 1, anak.size) }
        } else {
            juzTerbuka.remove(no)
            adapter.hapus(pos + 1, Data.anakJuz(no).size)
        }
        lm.findViewByPosition(pos)?.findViewById<View>(R.id.panah)
            ?.animate()?.rotation(if (buka) -90f else 90f)?.setDuration(180)?.start()
    }

    private fun munculkan(lm: LinearLayoutManager, dari: Int, jumlah: Int) {
        for (i in 0 until jumlah) {
            val v = lm.findViewByPosition(dari + i) ?: continue
            v.alpha = 0f
            v.translationY = -v.height * 0.6f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setStartDelay(i * 40L)
                .start()
        }
    }

    private fun buka(item: Entry) {
        startActivity(
            Intent(this, ReaderActivity::class.java)
                .putExtra(ReaderActivity.EXTRA_TITLE, item.title)
                .putExtra(ReaderActivity.EXTRA_PAGE, item.halaman ?: -1)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pager = findViewById(R.id.pager)
        indicator = findViewById(R.id.tab_indicator)

        pager.adapter = PageAdapter()
        pager.offscreenPageLimit = tabIds.size

        // Garis penanda mengikuti geseran halaman secara halus.
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
                indicator.translationX = (position + offset) * lebarTab
            }

            override fun onPageSelected(position: Int) {
                gayaTab(position)
            }
        })

        // Garis penanda selebar satu tab.
        findViewById<View>(R.id.tab_row).addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val w = v.width / tabIds.size
            if (w != lebarTab) {
                lebarTab = w
                indicator.layoutParams = indicator.layoutParams.apply { width = w }
                indicator.translationX = pager.currentItem * w.toFloat()
            }
        }

        // Ketuk tab: halaman bergeser dengan animasi.
        tabIds.forEachIndexed { index, id ->
            findViewById<View>(id).setOnClickListener { pager.setCurrentItem(index, true) }
        }
        gayaTab(pager.currentItem)
    }

    override fun onResume() {
        super.onResume()
        if (!FolderStore.isReadable(this)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        // Muat ulang penanda, supaya penanda baru dari layar baca langsung tampil.
        pager.adapter?.notifyItemChanged(2)
    }

    private fun gayaTab(aktif: Int) {
        tabIds.forEachIndexed { index, id ->
            val label = findViewById<TextView>(id)
            val selected = index == aktif
            label.setTextColor(getColor(if (selected) R.color.text else R.color.muted))
            label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adapter = EntryAdapter(
            onClick = { buka(it) },
            onToggle = { togglePanah(it) },
            terbuka = { juzTerbuka.contains(it) }
        )
        val rv: RecyclerView = view.findViewById(R.id.list)
        val kosong: TextView = view.findViewById(R.id.empty)

        init {
            rv.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@PageHolder.adapter
                // Tanpa animasi bawaan, supaya buka-tutup juz tidak berkedip.
                itemAnimator = null
            }
        }
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageHolder>() {
        override fun getItemCount() = tabIds.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.page_list, parent, false)
            return PageHolder(view)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val list = daftar(position)
            if (position == 1) {
                juzAdapter = holder.adapter
                juzRv = holder.rv
            }
            holder.adapter.submit(list)
            holder.kosong.visibility = if (position == 2 && list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
