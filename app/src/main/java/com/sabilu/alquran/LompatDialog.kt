package com.sabilu.alquran

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.BaseAdapter
import android.widget.FrameLayout
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
        val wadah = FrameLayout(ctx)
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_lompat, wadah, false)
        v.isClickable = true // sentuhan di dalam menu tidak menutup dialog
        v.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (TINGGI_DP * ctx.resources.displayMetrics.density).toInt(),
            Gravity.BOTTOM
        )
        wadah.addView(v)
        wadah.setOnClickListener { dialog.dismiss() } // ketuk area di atas menu: tutup
        dialog.setContentView(wadah)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.55f)
        }

        val tabJuz = v.findViewById<TextView>(R.id.tab_juz)
        val tabSurat = v.findViewById<TextView>(R.id.tab_surat)
        val panelJuz = v.findViewById<View>(R.id.panel_juz)
        val panelSurat = v.findViewById<View>(R.id.panel_surat)
        val btnJuz = v.findViewById<View>(R.id.btn_juz)
        val teksJuz = v.findViewById<TextView>(R.id.teks_juz)
        val panahJuz = v.findViewById<View>(R.id.panah_juz)
        val listJuz = v.findViewById<ListView>(R.id.list_juz)
        val fdHal = v.findViewById<View>(R.id.fd_hal)
        val inpJuzHal = v.findViewById<EditText>(R.id.inp_juz_hal)
        val panahHal = v.findViewById<View>(R.id.panah_hal)
        val listHal = v.findViewById<ListView>(R.id.list_hal)
        val hintJuz = v.findViewById<TextView>(R.id.hint_juz)
        val inpHalaman = v.findViewById<EditText>(R.id.inp_halaman)
        val btnSurat = v.findViewById<View>(R.id.btn_surat)
        val teksSurat = v.findViewById<TextView>(R.id.teks_surat)
        val panahSurat = v.findViewById<View>(R.id.panah_surat)
        val listSurat = v.findViewById<ListView>(R.id.list_surat)
        val inpAyat = v.findViewById<EditText>(R.id.inp_ayat)
        val hintAyat = v.findViewById<TextView>(R.id.hint_ayat)
        val d = ctx.resources.displayMetrics.density

        val daftarJuz = Data.daftarJuz
        val daftarSurat = Data.daftarSurat
        // Juz dan surat awal mengikuti halaman yang sedang dibaca.
        var juz = daftarJuz.lastOrNull { Data.halamanJuz(it) <= halamanSekarang } ?: 1
        var surat = daftarSurat.lastOrNull { Data.halamanSurat(it.first) <= halamanSekarang }?.first ?: 1
        var tab = Tab.JUZ

        /** Jumlah halaman di dalam satu juz (Juz 1 dan Juz 30 sedikit lebih banyak dari 20). */
        fun jumlahHalJuz(j: Int): Int =
            (if (j >= 30) Data.JUMLAH_HALAMAN else Data.halamanJuz(j + 1) - 1) - Data.halamanJuz(j) + 1

        // Halaman di dalam juz awal: posisi halaman yang sedang dibaca.
        inpJuzHal.setText((halamanSekarang - Data.halamanJuz(juz) + 1).coerceIn(1, jumlahHalJuz(juz)).toString())

        // Keterangan di bawah dropdown. Kolom kosong: tidak ada peringatan (kecuali [paksa], saat Lompat ditekan).
        // Placeholder nomor halaman mengikuti tujuan juz + halaman, atau halaman yang sedang dibaca.
        fun perbaruiHintJuz(paksa: Boolean = false) {
            val maks = jumlahHalJuz(juz)
            val n = inpJuzHal.text.toString().trim().toIntOrNull()
            val manual = inpHalaman.text.toString().trim()
            val tujuan = if (n != null && n in 1..maks) Data.halamanJuz(juz) + n - 1 else null
            inpHalaman.hint = (tujuan ?: halamanSekarang.coerceIn(1, Data.JUMLAH_HALAMAN)).toString()
            val peringatan = "Isi halaman 1 sampai $maks di juz ini."
            val merah = Color.parseColor("#E6847F")
            when {
                // Nomor halaman terisi: ia yang dipakai, apa pun juz dan halamannya.
                manual.isNotEmpty() -> {
                    hintJuz.setTextColor(ctx.getColor(R.color.muted))
                    hintJuz.text = "Buka halaman mushaf ${manual.toIntOrNull() ?: ""} (dari nomor halaman)."
                }
                inpJuzHal.text.isEmpty() -> {
                    hintJuz.setTextColor(if (paksa) merah else ctx.getColor(R.color.muted))
                    hintJuz.text = if (paksa) peringatan else ""
                }
                tujuan == null -> {
                    hintJuz.setTextColor(merah)
                    hintJuz.text = peringatan
                }
                else -> {
                    hintJuz.setTextColor(ctx.getColor(R.color.muted))
                    hintJuz.text = "Buka halaman mushaf $tujuan (halaman $n di Juz $juz)."
                }
            }
        }

        // Isi dropdown halaman: awal tiap seperempat juz (5 halaman).
        val itemHal = (0..3).map { "¼ — ${it + 1}" to "hal ${it * 5 + 1}" }
        val itemJuz = daftarJuz.map { "Juz $it" to "hal ${Data.halamanJuz(it)}" }
        val itemSurat = daftarSurat.map { (no, nama) -> "$no. $nama" to "${Data.jumlahAyat(no)} ayat" }
        listJuz.adapter = BarisAdapter(ctx, itemJuz) { juz - 1 }
        listHal.adapter = BarisAdapter(ctx, itemHal) {
            val n = inpJuzHal.text.toString().trim().toIntOrNull()
            if (n != null && n in 1..16 && (n - 1) % 5 == 0) (n - 1) / 5 else -1
        }
        listSurat.adapter = BarisAdapter(ctx, itemSurat) { surat - 1 }

        fun tampilJuz() {
            teksJuz.text = "Juz $juz"
        }

        fun gantiJuz(n: Int) {
            juz = n
            tampilJuz()
            // Halaman yang sudah diisi tetap dipakai, kecuali melewati jumlah halaman juz baru.
            val h = inpJuzHal.text.toString().trim().toIntOrNull()
            if (h != null && h > jumlahHalJuz(juz)) inpJuzHal.setText("1")
            perbaruiHintJuz()
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

        // ---- Dropdown menempel ke kotaknya, terbuka dan tertutup dengan animasi singkat ----
        class Dd(val list: ListView, val kotak: View, val panah: View, val jumlah: Int) {
            var anim: ValueAnimator? = null
            var sasaran = false
        }
        val ddJuz = Dd(listJuz, btnJuz, panahJuz, 30)
        val ddHal = Dd(listHal, fdHal, panahHal, 4)
        val ddSurat = Dd(listSurat, btnSurat, panahSurat, 114)
        var terbuka: Dd? = null

        fun sembunyikanKeyboard() {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            v.findFocus()?.clearFocus()
        }

        fun animasi(dd: Dd, buka: Boolean, instan: Boolean = false) {
            dd.anim?.cancel()
            dd.sasaran = buka
            val lp = dd.list.layoutParams
            val target = if (buka) (minOf(dd.jumlah * 38, 198) * d + 8 * d).toInt() else 0
            val dari = if (dd.list.visibility == View.VISIBLE) lp.height else 0
            if (buka) {
                dd.list.visibility = View.VISIBLE
                dd.kotak.setBackgroundResource(R.drawable.bg_isi_buka)
            }
            dd.panah.animate().cancel()
            dd.panah.animate().rotation(if (buka) -90f else 90f).setDuration(if (instan) 0 else 150).start()
            if (instan) {
                lp.height = target
                dd.list.layoutParams = lp
                if (!buka) {
                    dd.list.visibility = View.GONE
                    dd.kotak.setBackgroundResource(R.drawable.bg_isi)
                }
                return
            }
            dd.anim = ValueAnimator.ofInt(dari, target).apply {
                duration = 150
                interpolator = DecelerateInterpolator(1.5f)
                addUpdateListener {
                    lp.height = it.animatedValue as Int
                    dd.list.layoutParams = lp
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
                        if (!dd.sasaran) {
                            dd.list.visibility = View.GONE
                            dd.kotak.setBackgroundResource(R.drawable.bg_isi)
                        }
                    }
                })
                start()
            }
        }

        fun tutupDropdown(instan: Boolean = false) {
            terbuka?.let { animasi(it, false, instan) }
            terbuka = null
        }

        fun bukaDropdown(dd: Dd, posisiAktif: Int) {
            if (terbuka === dd) {
                tutupDropdown()
                return
            }
            sembunyikanKeyboard()
            tutupDropdown()
            animasi(dd, true)
            terbuka = dd
            dd.list.setSelection((posisiAktif - 2).coerceAtLeast(0))
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
            tutupDropdown(instan = true)
        }

        listJuz.setOnItemClickListener { _, _, pos, _ ->
            gantiJuz(pos + 1)
            tutupDropdown()
        }
        listHal.setOnItemClickListener { _, _, pos, _ ->
            inpJuzHal.setText((pos * 5 + 1).toString())
            inpJuzHal.setSelection(inpJuzHal.text.length)
            (listHal.adapter as BaseAdapter).notifyDataSetChanged()
            tutupDropdown()
        }
        listSurat.setOnItemClickListener { _, _, pos, _ ->
            gantiSurat(pos + 1)
            tutupDropdown()
        }
        btnJuz.setOnClickListener { bukaDropdown(ddJuz, juz - 1) }
        v.findViewById<View>(R.id.btn_panah_hal).setOnClickListener {
            bukaDropdown(ddHal, -1)
        }
        btnSurat.setOnClickListener { bukaDropdown(ddSurat, surat - 1) }
        // Mengetik di kotak halaman: dropdown menutup.
        inpJuzHal.setOnFocusChangeListener { _, fokus -> if (fokus) tutupDropdown() }
        inpHalaman.setOnFocusChangeListener { _, fokus -> if (fokus) tutupDropdown() }
        inpAyat.setOnFocusChangeListener { _, fokus -> if (fokus) tutupDropdown() }

        val pengamat = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = perbaruiHintJuz()
        }
        inpJuzHal.addTextChangedListener(pengamat)
        inpHalaman.addTextChangedListener(pengamat)

        tabJuz.setOnClickListener { tampilkanTab(Tab.JUZ) }
        tabSurat.setOnClickListener { tampilkanTab(Tab.SURAT) }

        v.findViewById<View>(R.id.btn_batal).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btn_lompat).setOnClickListener {
            when (tab) {
                Tab.JUZ -> {
                    val manual = inpHalaman.text.toString().trim()
                    if (manual.isNotEmpty()) {
                        // Nomor halaman mushaf selalu didahulukan, tidak peduli juz dan halaman di atasnya.
                        val n = manual.toIntOrNull()
                        if (n == null || n !in 1..Data.JUMLAH_HALAMAN) {
                            Toast.makeText(ctx, "Isi nomor halaman 1 sampai ${Data.JUMLAH_HALAMAN}", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        onHalaman(n)
                    } else {
                        val h = inpJuzHal.text.toString().trim().toIntOrNull()
                        if (h == null || h !in 1..jumlahHalJuz(juz)) {
                            // Halaman kosong: peringatan baru muncul di sini.
                            perbaruiHintJuz(paksa = true)
                            return@setOnClickListener
                        }
                        onHalaman(Data.halamanJuz(juz) + h - 1)
                    }
                }
                Tab.SURAT -> {
                    // Ayat lebih besar dari jumlah ayat surat: langsung ke ayat terakhir surat itu.
                    val n = inpAyat.text.toString().toIntOrNull() ?: 1
                    onAyat(surat, n.coerceIn(1, Data.jumlahAyat(surat)))
                }
            }
            dialog.dismiss()
        }

        tampilJuz()
        tampilSurat()
        perbaruiHintJuz()
        tampilkanTab(Tab.JUZ)
        dialog.show()

        // Pegangan di atas: digeser ke atas untuk memperbesar menu, ke bawah untuk menutup.
        // Kalau digeser ke bawah kurang jauh, menu kembali ke posisi semula.
        val tinggiPx = TINGGI_DP * ctx.resources.displayMetrics.density
        val maksPx = ctx.resources.displayMetrics.heightPixels * 0.9f
        var tinggiSekarang = tinggiPx
        var tinggiAwal = tinggiPx
        var mulaiY = 0f
        fun setTinggi(px: Float) {
            v.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                px.toInt(),
                Gravity.BOTTOM
            )
        }
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
                    setTinggi(tinggiSekarang)
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
