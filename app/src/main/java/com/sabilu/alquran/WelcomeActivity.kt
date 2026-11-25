package com.sabilu.alquran

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Layar pertama. Kalau file PDF Quran sudah dipilih dan masih terbaca, langsung ke layar utama.
 * Kalau belum, tampil tombol "Pilih file PDF Quran". Kalau sebelumnya pernah dipilih tapi
 * sekarang tidak terbaca (kartu SD dicabut, file dipindah dsb.), tampil pesan galat.
 */
class WelcomeActivity : AppCompatActivity() {

    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, R.string.izin_gagal, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            PdfStore.save(this, uri)
            openMain()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PdfStore.isReadable(this)) {
            openMain()
            return
        }

        setContentView(R.layout.activity_welcome)

        val pernahPilih = PdfStore.get(this) != null
        if (pernahPilih) tampilkanGalat()

        findViewById<android.view.View>(R.id.btn_pick).setOnClickListener {
            picker.launch(arrayOf("application/pdf"))
        }
    }

    private fun tampilkanGalat() {
        val warn = getColor(R.color.warn)
        findViewById<TextView>(R.id.welcome_title).setText(R.string.galat_judul)
        findViewById<TextView>(R.id.welcome_desc).setText(R.string.galat_isi)
        findViewById<TextView>(R.id.btn_pick).setText(R.string.galat_tombol)
        findViewById<TextView>(R.id.welcome_note).setText(R.string.galat_catatan)
        findViewById<ImageView>(R.id.ic_root).imageTintList = ColorStateList.valueOf(warn)
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
