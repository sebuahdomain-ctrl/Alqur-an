package com.sabilu.alquran

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface

/**
 * Menu Lompat (pop-up dari bawah). Tinggi menu tetap 420dp di kedua tab.
 * Tab Juz: dropdown juz dan nomor halaman. Tab Surat: dropdown surat dan nomor ayat.
 */
object LompatDialog {

    private enum class Tab { JUZ, SURAT }

    private const val TINGGI_DP = 420

    fun tampilkan(
        ctx: Context,
        halamanSekarang: Int,
        onHalaman: (Int) -> Unit,
        onAyat: (Int, Int) -> Unit
    ) {
        val dialog = Dialog(ctx)
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_lompat, null)
        dialog.setContentView(v)
        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (TINGGI_DP * ctx.resources.displayMetrics.density).toInt()
            )
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.55f)
        }

        val tabJuz = v.findViewById<TextView>(R.id.tab_juz)
        val tabSurat = v.findViewById<TextView>(R.id.tab_surat)
        val panelJuz = v.findViewById<View>(R.id.panel_juz)
        val panelSurat = v.findViewById<View>(R.id.panel_surat)
        val btnJuz = v.findViewById<View>(R.id.btn_juz)
        val teksJuz = v.findViewById<TextView>(R.id.teks_juz)
        val panahJuz = v.findViewById<TextView>(R.id.panah_juz)
        val listJuz = v.findViewById<ListView>(R.id.list_juz)
        val inpHalaman = v.findViewById<EditText>(R.id.inp_halaman)
        val btnSurat = v.findViewById<View>(R.id.btn_surat)
        val teksSurat = v.findViewById<TextView>(R.id.teks_surat)
        val panahSurat = v.findViewById<TextView>(R.id.panah_surat)
        val listSurat = v.findViewById<ListView>(R.id.list_surat)
        val inpAyat = v.findViewById<EditText>(R.id.inp_ayat)
        val hintAyat = v.findViewById<TextView>(R.id.hint_ayat)

        val daftarJuz = Data.daftarJuz
        val daftarSurat = Data.daftarSurat
        // Juz dan surat awal mengikuti halaman yang sedang dibaca.
        var juz = daftarJuz.lastOrNull { Data.halamanJuz(it) <= halamanSekarang } ?: 1
        var surat = daftarSurat.lastOrNull { Data.halamanSurat(it.first) <= halamanSekarang }?.first ?: 1
        var tab = Tab.JUZ

        // Placeholder (teks abu-abu) halaman: halaman yang sedang dibaca.
        // Berubah ke halaman awal juz kalau juz diganti dan kolom halaman belum diisi.
        inpHalaman.hint = halamanSekarang.coerceIn(1, Data.JUMLAH_HALAMAN).toString()
        inpAyat.hint = "1"

        val itemJuz = daftarJuz.map { "Juz $it" to "hal ${Data.halamanJuz(it)}" }
        val itemSurat = daftarSurat.map { (no, nama) -> "$no. $nama" to "${Data.jumlahAyat(no)} ayat" }
        listJuz.adapter = BarisAdapter(ctx, itemJuz) { juz - 1 }
        listSurat.adapter = BarisAdapter(ctx, itemSurat) { surat - 1 }

        fun tampilJuz() {
            teksJuz.text = "Juz $juz"
        }

        fun gantiJuz(n: Int) {
            juz = n
            tampilJuz()
            if (inpHalaman.text.isEmpty()) inpHalaman.hint = Data.halamanJuz(juz).toString()
            (listJuz.adapter as BaseAdapter).notifyDataSetChanged()
        }

        fun tampilSurat() {
            val (no, nama) = daftarSurat.first { it.first == surat }
            teksSurat.text = "$no. $nama"
            hintAyat.text = "Ayat 1 sampai ${Data.jumlahAyat(no)}"
        }

        fun gantiSurat(n: Int) {
            surat = n
            tampilSurat()
            (listSurat.adapter as BaseAdapter).notifyDataSetChanged()
        }

        fun tutupDropdown() {
            listJuz.visibility = View.GONE
            listSurat.visibility = View.GONE
            panahJuz.text = ctx.getString(R.string.lompat_panah_bawah)
            panahSurat.text = ctx.getString(R.string.lompat_panah_bawah)
        }

        fun bukaDropdown(list: ListView, panah: TextView) {
            val buka = list.visibility != View.VISIBLE
            tutupDropdown()
            if (buka) {
                list.visibility = View.VISIBLE
                panah.text = ctx.getString(R.string.lompat_panah_atas)
            }
        }

        fun gayaTab(tv: TextView, on: Boolean) {
            tv.setBackgroundResource(if (on) R.drawable.bg_tab_on else R.drawable.bg_tab_off)
            tv.setTextColor(if (on) Color.parseColor("#0F1210") else ctx.getColor(R.color.muted))
            tv.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
        }

        fun tampilkanTab(t: Tab) {
            tab = t
            panelJuz.visibility = if (t == Tab.JUZ) View.VISIBLE else View.GONE
            panelSurat.visibility = if (t == Tab.SURAT) View.VISIBLE else View.GONE
            gayaTab(tabJuz, t == Tab.JUZ)
            gayaTab(tabSurat, t == Tab.SURAT)
            tutupDropdown()
        }

        listJuz.setOnItemClickListener { _, _, pos, _ ->
            gantiJuz(pos + 1)
            tutupDropdown()
        }
        listSurat.setOnItemClickListener { _, _, pos, _ ->
            gantiSurat(pos + 1)
            tutupDropdown()
        }
        btnJuz.setOnClickListener { bukaDropdown(listJuz, panahJuz) }
        btnSurat.setOnClickListener { bukaDropdown(listSurat, panahSurat) }

        tabJuz.setOnClickListener { tampilkanTab(Tab.JUZ) }
        tabSurat.setOnClickListener { tampilkanTab(Tab.SURAT) }

        v.findViewById<View>(R.id.btn_batal).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btn_lompat).setOnClickListener {
            when (tab) {
                Tab.JUZ -> {
                    // Kolom kosong: pakai placeholder (halaman sekarang, atau halaman awal juz).
                    val teks = if (inpHalaman.text.isEmpty()) {
                        inpHalaman.hint?.toString() ?: ""
                    } else {
                        inpHalaman.text.toString()
                    }
                    val n = teks.toIntOrNull()
                    if (n == null || n !in 1..Data.JUMLAH_HALAMAN) {
                        Toast.makeText(ctx, "Isi nomor halaman 1 sampai ${Data.JUMLAH_HALAMAN}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    onHalaman(n)
                }
                Tab.SURAT -> {
                    // Ayat lebih besar dari jumlah ayat surat: langsung ke ayat terakhir surat itu.
                    val n = inpAyat.text.toString().toIntOrNull() ?: 1
                    onAyat(surat, n.coerceIn(1, Data.jumlahAyat(surat)))
                }
            }
            dialog.dismiss()
        }

        // Pegangan di atas: digeser ke atas untuk memperbesar menu, ke bawah untuk menutup.
        // Kalau digeser ke bawah kurang jauh, menu kembali ke posisi semula.
        val tinggiPx = TINGGI_DP * ctx.resources.displayMetrics.density
        val maksPx = ctx.resources.displayMetrics.heightPixels * 0.9f
        var tinggiSekarang = tinggiPx
        var tinggiAwal = tinggiPx
        var mulaiY = 0f
        v.findViewById<View>(R.id.pegangan).setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mulaiY = e.rawY
                    tinggiAwal = tinggiSekarang
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val target = tinggiAwal - (e.rawY - mulaiY)
                    if (target >= tinggiPx) {
                        tinggiSekarang = target.coerceAtMost(maksPx)
                        v.translationY = 0f
                    } else {
                        tinggiSekarang = tinggiPx
                        v.translationY = tinggiPx - target
                    }
                    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, tinggiSekarang.toInt())
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (v.translationY > tinggiPx * 0.3f) {
                        v.animate().translationY(tinggiSekarang).setDuration(160).withEndAction { dialog.dismiss() }.start()
                    } else {
                        v.animate().translationY(0f).setDuration(160).start()
                    }
                    true
                }
                else -> false
            }
        }

        tampilJuz()
        tampilSurat()
        tampilkanTab(Tab.JUZ)
        dialog.show()
    }

    /** Baris dropdown: nama di kiri, keterangan di kanan. Baris yang dipilih diberi garis hijau. */
    private class BarisAdapter(
        ctx: Context,
        private val data: List<Pair<String, String>>,
        private val aktif: () -> Int
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(ctx)

        override fun getCount() = data.size
        override fun getItem(position: Int): Any = data[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: inflater.inflate(R.layout.item_lompat, parent, false)
            v.findViewById<TextView>(R.id.nama).text = data[position].first
            v.findViewById<TextView>(R.id.info).text = data[position].second
            v.isSelected = position == aktif()
            return v
        }
    }
}
