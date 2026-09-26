package com.example.data.remote

object GenrePresets {

    data class GenreItem(
        val name: String,
        val searchQuery: String
    )

    val GENRES = listOf(
        GenreItem("TRENDING", "trending music official"),
        GenreItem("TECHNO", "techno music live set"),
        GenreItem("SYNTHWAVE", "synthwave retro electro 80s"),
        GenreItem("INDUSTRIAL", "industrial rock music official"),
        GenreItem("PUNK", "punk rock music official"),
        GenreItem("HIP HOP", "hip hop official music video"),
        GenreItem("LO-FI", "lofi hip hop chill beats to study relax"),
        GenreItem("METAL", "heavy metal official audio"),
        GenreItem("EDM", "electronic dance music hits")
    )
}
