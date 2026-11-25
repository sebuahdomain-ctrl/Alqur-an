package com.sabilu.alquran

/**
 * Satu baris daftar. [halaman] = nomor halaman mushaf (1 sampai 604) yang dibuka saat baris diketuk;
 * null kalau tidak punya tujuan (mis. penanda contoh).
 */
data class Entry(val title: String, val sub: String? = null, val halaman: Int? = null)

object Data {

    /** Jumlah halaman Mushaf Madinah. */
    const val JUMLAH_HALAMAN = 604

    // Nomor halaman mushaf tempat tiap surat dan juz dimulai.
    private val SURAT = listOf(
        Triple(1, "Al-Fatihah", 1),
        Triple(2, "Al-Baqarah", 2),
        Triple(3, "Ali Imran", 50),
        Triple(4, "An-Nisa'", 77),
        Triple(5, "Al-Ma'idah", 106),
        Triple(6, "Al-An'am", 128),
        Triple(7, "Al-A'raf", 151),
        Triple(8, "Al-Anfal", 177),
        Triple(9, "At-Taubah", 187),
        Triple(10, "Yunus", 208),
        Triple(11, "Hud", 221),
        Triple(12, "Yusuf", 235),
        Triple(13, "Ar-Ra'd", 249),
        Triple(14, "Ibrahim", 255),
        Triple(15, "Al-Hijr", 262),
        Triple(16, "An-Nahl", 267),
        Triple(17, "Al-Isra'", 282),
        Triple(18, "Al-Kahf", 293),
        Triple(19, "Maryam", 305),
        Triple(20, "Taha", 312),
        Triple(21, "Al-Anbiya'", 322),
        Triple(22, "Al-Hajj", 332),
        Triple(23, "Al-Mu'minun", 342),
        Triple(24, "An-Nur", 350),
        Triple(25, "Al-Furqan", 359),
        Triple(26, "Asy-Syu'ara'", 367),
        Triple(27, "An-Naml", 377),
        Triple(28, "Al-Qasas", 385),
        Triple(29, "Al-'Ankabut", 396),
        Triple(30, "Ar-Rum", 404),
        Triple(31, "Luqman", 411),
        Triple(32, "As-Sajdah", 415),
        Triple(33, "Al-Ahzab", 418),
        Triple(34, "Saba'", 428),
        Triple(35, "Fatir", 434),
        Triple(36, "Yasin", 440),
        Triple(37, "As-Saffat", 446),
        Triple(38, "Sad", 453),
        Triple(39, "Az-Zumar", 458),
        Triple(40, "Gafir", 467),
        Triple(41, "Fussilat", 477),
        Triple(42, "Asy-Syura", 483),
        Triple(43, "Az-Zukhruf", 489),
        Triple(44, "Ad-Dukhan", 496),
        Triple(45, "Al-Jasiyah", 499),
        Triple(46, "Al-Ahqaf", 502),
        Triple(47, "Muhammad", 507),
        Triple(48, "Al-Fath", 511),
        Triple(49, "Al-Hujurat", 515),
        Triple(50, "Qaf", 518),
        Triple(51, "Az-Zariyat", 520),
        Triple(52, "At-Tur", 523),
        Triple(53, "An-Najm", 526),
        Triple(54, "Al-Qamar", 528),
        Triple(55, "Ar-Rahman", 531),
        Triple(56, "Al-Waqi'ah", 534),
        Triple(57, "Al-Hadid", 537),
        Triple(58, "Al-Mujadalah", 542),
        Triple(59, "Al-Hasyr", 545),
        Triple(60, "Al-Mumtahanah", 549),
        Triple(61, "As-Saff", 551),
        Triple(62, "Al-Jumu'ah", 553),
        Triple(63, "Al-Munafiqun", 554),
        Triple(64, "At-Tagabun", 556),
        Triple(65, "At-Talaq", 558),
        Triple(66, "At-Tahrim", 560),
        Triple(67, "Al-Mulk", 562),
        Triple(68, "Al-Qalam", 564),
        Triple(69, "Al-Haqqah", 566),
        Triple(70, "Al-Ma'arij", 568),
        Triple(71, "Nuh", 570),
        Triple(72, "Al-Jinn", 572),
        Triple(73, "Al-Muzzammil", 574),
        Triple(74, "Al-Muddassir", 575),
        Triple(75, "Al-Qiyamah", 577),
        Triple(76, "Al-Insan", 578),
        Triple(77, "Al-Mursalat", 580),
        Triple(78, "An-Naba'", 582),
        Triple(79, "An-Nazi'at", 583),
        Triple(80, "'Abasa", 585),
        Triple(81, "At-Takwir", 586),
        Triple(82, "Al-Infitar", 587),
        Triple(83, "Al-Mutaffifin", 587),
        Triple(84, "Al-Insyiqaq", 589),
        Triple(85, "Al-Buruj", 590),
        Triple(86, "At-Tariq", 591),
        Triple(87, "Al-A'la", 591),
        Triple(88, "Al-Gasyiyah", 592),
        Triple(89, "Al-Fajr", 593),
        Triple(90, "Al-Balad", 594),
        Triple(91, "Asy-Syams", 595),
        Triple(92, "Al-Lail", 595),
        Triple(93, "Ad-Duha", 596),
        Triple(94, "Al-Insyirah", 596),
        Triple(95, "At-Tin", 597),
        Triple(96, "Al-'Alaq", 597),
        Triple(97, "Al-Qadr", 598),
        Triple(98, "Al-Bayyinah", 598),
        Triple(99, "Az-Zalzalah", 599),
        Triple(100, "Al-'Adiyat", 599),
        Triple(101, "Al-Qari'ah", 600),
        Triple(102, "At-Takasur", 600),
        Triple(103, "Al-'Asr", 601),
        Triple(104, "Al-Humazah", 601),
        Triple(105, "Al-Fil", 601),
        Triple(106, "Quraisy", 602),
        Triple(107, "Al-Ma'un", 602),
        Triple(108, "Al-Kausar", 602),
        Triple(109, "Al-Kafirun", 603),
        Triple(110, "An-Nasr", 603),
        Triple(111, "Al-Lahab", 603),
        Triple(112, "Al-Ikhlas", 604),
        Triple(113, "Al-Falaq", 604),
        Triple(114, "An-Nas", 604),
    )

    private val JUZ = listOf(
        1 to 1,
        2 to 22,
        3 to 42,
        4 to 62,
        5 to 82,
        6 to 102,
        7 to 121,
        8 to 142,
        9 to 162,
        10 to 182,
        11 to 201,
        12 to 222,
        13 to 242,
        14 to 262,
        15 to 282,
        16 to 302,
        17 to 322,
        18 to 342,
        19 to 362,
        20 to 382,
        21 to 402,
        22 to 422,
        23 to 442,
        24 to 462,
        25 to 482,
        26 to 502,
        27 to 522,
        28 to 542,
        29 to 562,
        30 to 582,
    )

    // Format judul: "1. Al-Fatihah".
    val surat: List<Entry> = SURAT.map { (no, nama, hal) -> Entry("$no. $nama", null, hal) }

    val juz: List<Entry> = JUZ.map { (no, hal) -> Entry("Juz $no", null, hal) }

    /**
     * Judul dan keterangan bilah atas untuk satu halaman mushaf (1 sampai 604):
     * surat dan juz terakhir yang sudah dimulai sampai halaman itu, plus nomor halamannya.
     */
    fun infoHalaman(halaman: Int): Pair<String, String?> {
        val h = halaman.coerceIn(1, JUMLAH_HALAMAN)
        val (no, nama, _) = SURAT.last { it.third <= h }
        val juzNo = JUZ.last { it.second <= h }.first
        return "$no. $nama" to "Halaman $h, Juz $juzNo"
    }

    /** Baris daftar untuk satu halaman mushaf (dipakai tab Penanda). */
    fun entri(halaman: Int): Entry {
        val (judul, ket) = infoHalaman(halaman)
        return Entry(judul, ket, halaman)
    }
}
