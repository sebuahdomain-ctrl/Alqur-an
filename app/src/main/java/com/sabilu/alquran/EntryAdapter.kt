package com.sabilu.alquran

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EntryAdapter(
    private val onClick: (Entry) -> Unit,
    private val onToggle: (Entry) -> Unit = {},
    private val terbuka: (Int) -> Boolean = { false },
    /** true untuk tab Penanda: nomor halaman diganti simbol penanda yang bisa diketuk untuk menghapus. */
    private val modePenanda: Boolean = false,
    private val onHapus: (Entry) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Entry> = emptyList()

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    /** Posisi baris juz nomor [no], atau -1 kalau tidak ada. */
    fun posisiJuz(no: Int): Int = items.indexOfFirst { it.juz == no }

    /** Tambahkan baris [baru] tepat di bawah posisi [setelah]. Baris lain tidak dibangun ulang. */
    fun sisipkan(setelah: Int, baru: List<Entry>) {
        val daftar = items.toMutableList()
        daftar.addAll(setelah + 1, baru)
        items = daftar
        notifyItemRangeInserted(setelah + 1, baru.size)
    }

    /** Hapus [jumlah] baris mulai dari posisi [dari]. */
    fun hapus(dari: Int, jumlah: Int) {
        val daftar = items.toMutableList()
        repeat(jumlah) { daftar.removeAt(dari) }
        items = daftar
        notifyItemRangeRemoved(dari, jumlah)
    }

    /** Hapus satu baris [entri] dari daftar (dipakai saat penanda dihapus). */
    fun hapusEntri(entri: Entry) {
        val pos = items.indexOf(entri)
        if (pos < 0) return
        items = items.toMutableList().also { it.removeAt(pos) }
        notifyItemRemoved(pos)
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

        view.findViewById<TextView>(R.id.title).apply {
            text = item.title
            textSize = if (item.indent) 16f else 17f
        }
        val sub = view.findViewById<TextView>(R.id.sub)
        sub.text = item.sub
        sub.visibility = if (item.sub == null) View.GONE else View.VISIBLE

        val hal = view.findViewById<TextView>(R.id.halaman)
        hal.text = item.halaman?.toString() ?: ""
        // Tab Penanda: nomor halaman dihapus, diganti simbol penanda di paling kanan.
        hal.visibility = if (item.halaman == null || modePenanda) View.GONE else View.VISIBLE

        val tombol = view.findViewById<View>(R.id.tombol_penanda)
        tombol.visibility = if (modePenanda) View.VISIBLE else View.GONE
        tombol.setOnClickListener(if (modePenanda) View.OnClickListener { onHapus(item) } else null)

        // Baris surat di dalam juz: menjorok dan diberi latar sendiri.
        val isi = view.findViewById<View>(R.id.isi)
        val kiri = (if (item.indent) 44 else 20) * density
        isi.setPaddingRelative(kiri.toInt(), isi.paddingTop, (20 * density).toInt(), isi.paddingBottom)
        view.findViewById<View>(R.id.latar_anak).visibility = if (item.indent) View.VISIBLE else View.GONE

        // Juz yang punya surat di dalamnya: nomor halaman dan chevron bisa diketuk untuk membuka.
        val juz = item.juz
        val punyaAnak = juz != null && Data.anakJuz(juz).isNotEmpty()
        val terbukaSekarang = juz != null && terbuka(juz)
        val aksi = view.findViewById<View>(R.id.aksi)
        aksi.isClickable = punyaAnak
        aksi.setOnClickListener(if (punyaAnak) View.OnClickListener { onToggle(item) } else null)
        val panah = view.findViewById<View>(R.id.panah)
        panah.visibility = if (punyaAnak) View.VISIBLE else View.GONE
        // Chevron menghadap bawah saat tertutup, menghadap atas saat terbuka.
        panah.rotation = if (terbukaSekarang) -90f else 90f

        // Garis bawah selalu abu tipis, juz terbuka atau tertutup.
        val garis = view.findViewById<View>(R.id.garis_bawah)
        garis.setBackgroundColor(view.context.getColor(R.color.line))
        garis.layoutParams = garis.layoutParams.apply { height = density.toInt() }

        view.setOnClickListener { onClick(item) }
    }
}
