package com.sabilu.alquran

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val tabIds = listOf(R.id.tab_surat, R.id.tab_juz, R.id.tab_penanda)

    private var activeTab = 0
    private var scan: Scan? = null

    private val adapter = EntryAdapter { item ->
        startActivity(
            Intent(this, ReaderActivity::class.java)
                .putExtra(ReaderActivity.EXTRA_TITLE, item.title)
                .putExtra(ReaderActivity.EXTRA_URI, item.uri)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        tabIds.forEachIndexed { index, id ->
            findViewById<View>(id).setOnClickListener { setTab(index) }
        }
        setTab(0)
    }

    override fun onResume() {
        super.onResume()
        if (!FolderStore.isReadable(this)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        // Baca isi folder di thread latar supaya layar tidak macet.
        Thread {
            val hasil = Data.scan(this)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                scan = hasil
                setTab(activeTab)
            }
        }.start()
    }

    private fun setTab(active: Int) {
        activeTab = active
        tabIds.forEachIndexed { index, id ->
            val tab = findViewById<ViewGroup>(id)
            val label = tab.getChildAt(0) as TextView
            val line = tab.getChildAt(1)
            val selected = index == active
            label.setTextColor(getColor(if (selected) R.color.text else R.color.muted))
            label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            line.setBackgroundColor(if (selected) getColor(R.color.accent) else 0)
        }

        val s = scan
        val list: List<Entry>
        var pesan: String? = null
        when (active) {
            0 -> { list = s?.surat.orEmpty(); pesan = s?.suratGalat }
            1 -> { list = s?.juz.orEmpty(); pesan = s?.juzGalat }
            else -> list = Data.penanda()
        }
        adapter.submit(list)

        val empty = findViewById<TextView>(R.id.empty)
        empty.text = pesan
        empty.visibility = if (list.isEmpty() && pesan != null) View.VISIBLE else View.GONE
    }
}
