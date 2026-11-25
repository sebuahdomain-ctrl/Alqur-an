package com.sabilu.alquran

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Pencarian: nama surat, nomor (surat, juz, halaman), "juz N", "hal N", dan "surat:ayat".
 * Hasil muncul langsung saat mengetik, dikelompokkan per jenis.
 */
class SearchActivity : AppCompatActivity() {

    private sealed class Baris {
        class Judul(val teks: String) : Baris()
        class Hasil(val judul: String, val sub: String?, val kanan: String, val aksi: () -> Unit) : Baris()
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val adapter = HasilAdapter()
    private lateinit var kolom: EditText
    private lateinit var hapus: View
    private lateinit var kosong: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        kolom = findViewById(R.id.kolom)
        hapus = findViewById(R.id.hapus)
        kosong = findViewById(R.id.kosong)
        findViewById<RecyclerView>(R.id.daftar).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SearchActivity.adapter
            itemAnimator = null
        }

        findViewById<View>(R.id.kembali).setOnClickListener { finish() }
        hapus.setOnClickListener { kolom.setText("") }
        kolom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = cari(s?.toString().orEmpty())
        })
        kolom.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                // Ada satu hasil saja: langsung dibuka.
                adapter.hasilTunggal()?.aksi?.invoke()
                true
            } else false
        }
        cari("")
    }

    private fun cari(mentah: String) {
        val v = mentah.trim()
        hapus.visibility = if (mentah.isEmpty()) View.INVISIBLE else View.VISIBLE
        val baris = ArrayList<Baris>()
        var pesan: String? = null

        val ayat = Regex("^(\\d+)\\s*[:.]\\s*(\\d+)$").find(v)
        val juz = Regex("^juz\\s*(\\d+)$", RegexOption.IGNORE_CASE).find(v)
        val hal = Regex("^(?:hal|halaman)\\s*(\\d+)$", RegexOption.IGNORE_CASE).find(v)

        when {
            v.isEmpty() -> {
                baris.add(Baris.Judul(getString(R.string.cari_coba)))
                listOf("Baqarah", "36", "juz 30", "hal 604", "2:255").forEach { contoh ->
                    baris.add(Baris.Hasil(contoh, null, "") {
                        kolom.setText(contoh)
                        kolom.setSelection(contoh.length)
                    })
                }
            }
            ayat != null -> {
                val no = ayat.groupValues[1].toIntOrNull() ?: 0
                val a = ayat.groupValues[2].toIntOrNull() ?: 0
                if (no !in 1..114) {
                    pesan = "Surat $no tidak ada. Surat hanya 1 sampai 114."
                } else if (a !in 1..Data.jumlahAyat(no)) {
                    pesan = "${namaSurat(no)} hanya punya ${Data.jumlahAyat(no)} ayat."
                } else {
                    baris.add(Baris.Judul(getString(R.string.cari_ayat_judul)))
                    baris.add(Baris.Hasil("${namaSurat(no)} ayat $a", getString(R.string.cari_ayat_sub), "") { bukaAyat(no, a) })
                }
            }
            juz != null -> {
                val n = juz.groupValues[1].toIntOrNull() ?: 0
                if (n !in 1..30) pesan = "Juz hanya 1 sampai 30."
                else {
                    baris.add(Baris.Judul(getString(R.string.cari_juz_judul)))
                    baris.add(barisJuz(n))
                }
            }
            hal != null -> {
                val n = hal.groupValues[1].toIntOrNull() ?: 0
                if (n !in 1..Data.JUMLAH_HALAMAN) pesan = "Halaman hanya 1 sampai ${Data.JUMLAH_HALAMAN}."
                else {
                    baris.add(Baris.Judul(getString(R.string.cari_halaman_judul)))
                    baris.add(barisHalaman(n))
                }
            }
            v.all { it.isDigit() } -> {
                val n = v.toIntOrNull() ?: 0
                if (n in 1..114) {
                    baris.add(Baris.Judul(getString(R.string.cari_surat_judul)))
                    baris.add(barisSurat(n))
                }
                if (n in 1..30) {
                    baris.add(Baris.Judul(getString(R.string.cari_juz_judul)))
                    baris.add(barisJuz(n))
                }
                if (n in 1..Data.JUMLAH_HALAMAN) {
                    baris.add(Baris.Judul(getString(R.string.cari_halaman_judul)))
                    baris.add(barisHalaman(n))
                }
                if (baris.isEmpty()) pesan = "Tidak ada hasil untuk $v."
            }
            else -> {
                val kunci = norm(v)
                val cocok = Data.daftarSurat.filter { norm(it.second).contains(kunci) }
                if (cocok.isEmpty()) {
                    pesan = "Tidak ada surat bernama \"$v\"."
                } else {
                    baris.add(Baris.Judul(getString(R.string.cari_surat_judul)))
                    cocok.forEach { baris.add(barisSurat(it.first)) }
                }
            }
        }

        adapter.submit(baris)
        kosong.text = pesan.orEmpty()
        kosong.visibility = if (pesan != null) View.VISIBLE else View.GONE
    }

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun namaSurat(no: Int) = Data.daftarSurat.first { it.first == no }.second

    private fun barisSurat(no: Int) = Baris.Hasil(
        "$no. ${namaSurat(no)}", Data.jenisDanAyat(no), Data.halamanSurat(no).toString()
    ) { bukaHalaman(Data.halamanSurat(no)) }

    private fun barisJuz(n: Int) = Baris.Hasil(
        "Juz $n", null, Data.halamanJuz(n).toString()
    ) { bukaHalaman(Data.halamanJuz(n)) }

    private fun barisHalaman(n: Int) = Baris.Hasil(
        "Halaman $n", Data.infoHalaman(n).first, n.toString()
    ) { bukaHalaman(n) }

    private fun bukaHalaman(h: Int, sorot: String? = null) {
        Sorot.kunci = sorot
        startActivity(
            Intent(this, ReaderActivity::class.java)
                .putExtra(ReaderActivity.EXTRA_TITLE, "")
                .putExtra(ReaderActivity.EXTRA_PAGE, h)
        )
    }

    /** Cari halaman ayat di verses.json, lalu buka dengan ayat disorot hijau. */
    private fun bukaAyat(surat: Int, ayat: Int) {
        val app = applicationContext
        try {
            executor.execute {
                val h = AyatIndex.halamanAyat(app, surat, ayat)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (h == null) {
                        Toast.makeText(this, "Data ayat tidak ditemukan di folder Quran", Toast.LENGTH_LONG).show()
                    } else {
                        bukaHalaman(h, AyatIndex.kunci(surat, ayat))
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            // Layar sedang ditutup.
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private inner class HasilAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items: List<Baris> = emptyList()

        fun submit(list: List<Baris>) {
            items = list
            notifyDataSetChanged()
        }

        /** Satu-satunya hasil (selain contoh), atau null kalau hasilnya lebih dari satu. */
        fun hasilTunggal(): Baris.Hasil? {
            val h = items.filterIsInstance<Baris.Hasil>()
            return if (h.size == 1 && (h[0].kanan.isNotEmpty() || h[0].sub != null)) h[0] else null
        }

        override fun getItemCount() = items.size

        override fun getItemViewType(position: Int) = if (items[position] is Baris.Judul) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == 0) {
                val d = parent.resources.displayMetrics.density
                val t = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setTextColor(parent.context.getColor(R.color.accent))
                    textSize = 13f
                    setPadding((20 * d).toInt(), (16 * d).toInt(), (20 * d).toInt(), (6 * d).toInt())
                }
                return object : RecyclerView.ViewHolder(t) {}
            }
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
            return object : RecyclerView.ViewHolder(v) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val b = items[position]
            val v = holder.itemView
            if (b is Baris.Judul) {
                (v as TextView).text = b.teks
                return
            }
            b as Baris.Hasil
            v.findViewById<TextView>(R.id.title).text = b.judul
            val sub = v.findViewById<TextView>(R.id.sub)
            sub.text = b.sub
            sub.visibility = if (b.sub == null) View.GONE else View.VISIBLE
            val hal = v.findViewById<TextView>(R.id.halaman)
            hal.text = b.kanan
            hal.visibility = if (b.kanan.isEmpty()) View.GONE else View.VISIBLE
            v.findViewById<View>(R.id.panah).visibility = View.GONE
            v.findViewById<View>(R.id.tombol_penanda).visibility = View.GONE
            v.findViewById<View>(R.id.latar_anak).visibility = View.GONE
            v.setOnClickListener { b.aksi() }
        }
    }
}
