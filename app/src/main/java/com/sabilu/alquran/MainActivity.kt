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

    private val adapter = EntryAdapter { item ->
        startActivity(
            Intent(this, ReaderActivity::class.java)
                .putExtra(ReaderActivity.EXTRA_TITLE, item.title)
                .putExtra(ReaderActivity.EXTRA_PAGE, item.halaman ?: -1)
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
        if (!PdfStore.isReadable(this)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
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

        val list = when (active) {
            0 -> Data.surat
            1 -> Data.juz
            else -> Data.penanda()
        }
        adapter.submit(list)
        findViewById<TextView>(R.id.empty).visibility = View.GONE
    }
}
