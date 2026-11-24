package com.sabilu.alquran

sealed class Entry {
    data class Group(val title: String) : Entry()
    data class Row(val title: String, val sub: String? = null) : Entry()
}

object Data {
    // Tiap juz berisi surat yang dimulai di juz tersebut.
    private val juz = listOf(
        "Juz 1" to listOf("Al-Fatihah", "Al-Baqarah"),
        "Juz 2" to emptyList(),
        "Juz 3" to listOf("Ali ‘Imran"),
        "Juz 4" to listOf("An-Nisa’"),
        "Juz 5" to emptyList(),
        "Juz 6" to listOf("Al-Ma’idah"),
        "Juz 7" to listOf("Al-An’am"),
        "Juz 8" to listOf("Al-A’raf"),
        "Juz 9" to listOf("Al-Anfal"),
        "Juz 10" to listOf("At-Taubah"),
    )

    private val penanda = listOf(
        "Al-Baqarah" to "Halaman 12",
        "Juz 3" to "Halaman 5",
        "An-Nisa’" to "Halaman 3",
    )

    fun surat(): List<Entry> {
        val out = mutableListOf<Entry>()
        var nomor = 0
        for ((judul, daftar) in juz) {
            out += Entry.Group(judul)
            for (nama in daftar) {
                nomor++
                out += Entry.Row(nama, "Surat $nomor")
            }
        }
        return out
    }

    fun juz(): List<Entry> = juz.map { Entry.Row(it.first) }

    fun penanda(): List<Entry> = penanda.map { Entry.Row(it.first, it.second) }
}
