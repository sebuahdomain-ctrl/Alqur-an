package com.sabilu.alquran

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/**
 * Menu Lompat (pop-up dari bawah). Tinggi menu tetap 420dp di semua mode.
 * Mode Halaman dan Surat langsung bisa dipakai. Mode Ayat belum, karena data ayat ke halaman belum ada.
 */
object LompatDialog {

    private enum class Mode { HALAMAN, AYAT, SURAT }

    private const val TINGGI_DP = 420

    fun tampilkan(ctx: Context, halamanSekarang: Int, onLompat: (Int) -> Unit) {
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

        val tabHalaman = v.findViewById<TextView>(R.id.tab_halaman)
        val tabAyat = v.findViewById<TextView>(R.id.tab_ayat)
        val tabSurat = v.findViewById<TextView>(R.id.tab_surat)
        val panelHalaman = v.findViewById<View>(R.id.panel_halaman)
        val panelAyat = v.findViewById<View>(R.id.panel_ayat)
        val panelSurat = v.findViewById<View>(R.id.panel_surat)
        val inpHalaman = v.findViewById<EditText>(R.id.inp_halaman)
        val btnSurat = v.findViewById<View>(R.id.btn_surat)
        val teksSurat = v.findViewById<TextView>(R.id.teks_surat)
        val panahSurat = v.findViewById<TextView>(R.id.panah_surat)
        val listAyat = v.findViewById<ListView>(R.id.list_ayat)
        val inpAyat = v.findViewById<EditText>(R.id.inp_ayat)
        val hintAyat = v.findViewById<TextView>(R.id.hint_ayat)
        val listSurat = v.findViewById<ListView>(R.id.list_surat)

        val daftar = Data.daftarSurat
        // Surat awal mengikuti halaman yang sedang dibaca.
        var pilihan = daftar.lastOrNull { Data.halamanSurat(it.first) <= halamanSekarang }?.first ?: 1
        var mode = Mode.HALAMAN

        inpHalaman.setText(halamanSekarang.coerceIn(1, Data.JUMLAH_HALAMAN).toString())

        fun segarkanSurat() {
            val (no, nama) = daftar.first { it.first == pilihan }
            teksSurat.text = "$no. $nama"
            hintAyat.text = "Ayat 1 sampai ${Data.jumlahAyat(no)}"
            (listAyat.adapter as BaseAdapter).notifyDataSetChanged()
            (listSurat.adapter as BaseAdapter).notifyDataSetChanged()
        }

        fun gayaTab(tv: TextView, on: Boolean) {
            tv.setBackgroundResource(if (on) R.drawable.bg_tab_on else R.drawable.bg_tab_off)
            tv.setTextColor(if (on) Color.parseColor("#0F1210") else ctx.getColor(R.color.muted))
            tv.setTypeface(null, if (on) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        fun tutupDropdown() {
            listAyat.visibility = View.GONE
            panahSurat.text = ctx.getString(R.string.lompat_panah_bawah)
        }

        fun tampilkanMode(m: Mode) {
            mode = m
            panelHalaman.visibility = if (m == Mode.HALAMAN) View.VISIBLE else View.GONE
            panelAyat.visibility = if (m == Mode.AYAT) View.VISIBLE else View.GONE
            panelSurat.visibility = if (m == Mode.SURAT) View.VISIBLE else View.GONE
            gayaTab(tabHalaman, m == Mode.HALAMAN)
            gayaTab(tabAyat, m == Mode.AYAT)
            gayaTab(tabSurat, m == Mode.SURAT)
            tutupDropdown()
        }

        listAyat.adapter = DaftarSurat(ctx, daftar) { pilihan }
        listAyat.setOnItemClickListener { _, _, pos, _ ->
            pilihan = daftar[pos].first
            segarkanSurat()
            tutupDropdown()
        }
        listSurat.adapter = DaftarSurat(ctx, daftar) { pilihan }
        listSurat.setOnItemClickListener { _, _, pos, _ ->
            pilihan = daftar[pos].first
            segarkanSurat()
        }

        // Ketuk kolom surat: dropdown terbuka atau tertutup ke bawah.
        btnSurat.setOnClickListener {
            val buka = listAyat.visibility != View.VISIBLE
            listAyat.visibility = if (buka) View.VISIBLE else View.GONE
            panahSurat.text = ctx.getString(if (buka) R.string.lompat_panah_atas else R.string.lompat_panah_bawah)
        }

        tabHalaman.setOnClickListener { tampilkanMode(Mode.HALAMAN) }
        tabAyat.setOnClickListener { tampilkanMode(Mode.AYAT) }
        tabSurat.setOnClickListener { tampilkanMode(Mode.SURAT) }

        v.findViewById<View>(R.id.btn_batal).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btn_lompat).setOnClickListener {
            when (mode) {
                Mode.HALAMAN -> {
                    val n = inpHalaman.text.toString().toIntOrNull()
                    if (n == null || n !in 1..Data.JUMLAH_HALAMAN) {
                        Toast.makeText(ctx, "Isi nomor halaman 1 sampai ${Data.JUMLAH_HALAMAN}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    onLompat(n)
                }
                Mode.SURAT -> onLompat(Data.halamanSurat(pilihan))
                Mode.AYAT -> {
                    Toast.makeText(ctx, "Lompat ke ayat belum tersedia", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            dialog.dismiss()
        }

        segarkanSurat()
        tampilkanMode(Mode.HALAMAN)
        dialog.show()
    }

    /** Baris surat: nomor dan nama di kiri, jumlah ayat di kanan. Baris terpilih diberi garis hijau. */
    private class DaftarSurat(
        ctx: Context,
        private val data: List<Pair<Int, String>>,
        private val aktif: () -> Int
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(ctx)

        override fun getCount() = data.size
        override fun getItem(position: Int) = data[position]
        override fun getItemId(position: Int) = data[position].first.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: inflater.inflate(R.layout.item_lompat, parent, false)
            val (no, nama) = data[position]
            v.findViewById<TextView>(R.id.nama).text = "$no. $nama"
            v.findViewById<TextView>(R.id.info).text = "${Data.jumlahAyat(no)} ayat"
            v.isSelected = no == aktif()
            return v
        }
    }
}
