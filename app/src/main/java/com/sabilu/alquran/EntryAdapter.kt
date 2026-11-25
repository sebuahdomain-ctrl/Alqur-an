package com.sabilu.alquran

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EntryAdapter(
    private val onClick: (Entry) -> Unit,
    private val onToggle: (Entry) -> Unit = {},
    private val terbuka: (Int) -> Boolean = { false }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Entry> = emptyList()

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        val item = items[position]
        val density = view.resources.displayMetrics.density

        view.findViewById<TextView>(R.id.title).text = item.title
        val sub = view.findViewById<TextView>(R.id.sub)
        sub.text = item.sub
        sub.visibility = if (item.sub == null) View.GONE else View.VISIBLE

        val hal = view.findViewById<TextView>(R.id.halaman)
        hal.text = item.halaman?.toString() ?: ""
        hal.visibility = if (item.halaman == null) View.GONE else View.VISIBLE

        // Baris surat di dalam juz dibuat menjorok.
        val isi = view.findViewById<View>(R.id.isi)
        val kiri = (if (item.indent) 44 else 20) * density
        isi.setPaddingRelative(kiri.toInt(), isi.paddingTop, (20 * density).toInt(), isi.paddingBottom)

        // Chevron hanya untuk juz yang punya surat di dalamnya.
        val juz = item.juz
        val punyaAnak = juz != null && Data.anakJuz(juz).isNotEmpty()
        val panah = view.findViewById<View>(R.id.panah)
        panah.visibility = if (punyaAnak) View.VISIBLE else View.GONE
        // Chevron menghadap bawah saat tertutup, menghadap atas saat terbuka.
        panah.rotation = if (juz != null && terbuka(juz)) -90f else 90f
        panah.setOnClickListener { if (punyaAnak) onToggle(item) }

        view.setOnClickListener { onClick(item) }
    }
}
