package com.sabilu.alquran

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Layar pertama. Kalau folder data Quran (berisi pages dan fonts) sudah dipilih dan masih terbaca,
 * langsung ke layar utama. Kalau belum, tampil tombol "Pilih folder data Quran". Kalau sebelumnya
 * pernah dipilih tapi sekarang tidak terbaca (kartu SD dicabut, folder dipindah dsb.), tampil pesan galat.
 */
class WelcomeActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, R.string.izin_gagal, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            FolderStore.save(this, uri)
            periksaLaluBuka()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FolderStore.isReadable(this)) {
            openMain()
            return
        }

        setContentView(R.layout.activity_welcome)

        val pernahPilih = FolderStore.get(this) != null
        if (pernahPilih) tampilkanGalat()

        findViewById<android.view.View>(R.id.btn_pick).setOnClickListener {
            picker.launch(null)
        }
    }

    /** Pastikan folder yang dipilih berisi pages dan fonts. Kalau tidak, pilihan dibuang. */
    private fun periksaLaluBuka() {
        try {
            executor.execute {
                val ok = QuranFiles.siapkan(applicationContext)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (ok) {
                        openMain()
                    } else {
                        FolderStore.clear(this)
                        Toast.makeText(this, R.string.folder_salah, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            // Layar sudah ditutup.
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

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
