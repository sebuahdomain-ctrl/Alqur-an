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
        if (!juzTerbuka.add(no)) juzTerbuka.remove(no)
        pager.adapter?.notifyItemChanged(1)
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
        val kosong: TextView = view.findViewById(R.id.empty)

        init {
            view.findViewById<RecyclerView>(R.id.list).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@PageHolder.adapter
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
            holder.adapter.submit(list)
            holder.kosong.visibility = if (position == 2 && list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
