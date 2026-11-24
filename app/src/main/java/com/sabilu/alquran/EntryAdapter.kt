package com.sabilu.alquran

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EntryAdapter(private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Entry> = emptyList()

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        if (items[position] is Entry.Group) TYPE_GROUP else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_GROUP) R.layout.item_group else R.layout.item_row
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        when (val item = items[position]) {
            is Entry.Group -> {
                view.findViewById<TextView>(R.id.title).text = item.title
                view.setOnClickListener { onClick(item.title) }
            }
            is Entry.Row -> {
                view.findViewById<TextView>(R.id.title).text = item.title
                val sub = view.findViewById<TextView>(R.id.sub)
                sub.text = item.sub
                sub.visibility = if (item.sub == null) View.GONE else View.VISIBLE
                view.setOnClickListener { onClick(item.title) }
            }
        }
    }

    private companion object {
        const val TYPE_GROUP = 0
        const val TYPE_ROW = 1
    }
}
