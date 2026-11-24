package com.sabilu.alquran

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** Menyimpan dan memeriksa folder induk Quran yang dipilih user (lewat pemilih folder Android). */
object FolderStore {
    private const val PREF = "alquran"
    private const val KEY_URI = "folder_uri"

    fun save(ctx: Context, uri: Uri) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun get(ctx: Context): Uri? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)?.let { Uri.parse(it) }

    /** True kalau folder pernah dipilih, izinnya masih ada, dan isinya masih bisa dibaca. */
    fun isReadable(ctx: Context): Boolean {
        val uri = get(ctx) ?: return false
        return try {
            val granted = ctx.contentResolver.persistedUriPermissions
                .any { it.uri == uri && it.isReadPermission }
            val doc = DocumentFile.fromTreeUri(ctx, uri)
            granted && doc != null && doc.canRead()
        } catch (e: Exception) {
            false
        }
    }
}
