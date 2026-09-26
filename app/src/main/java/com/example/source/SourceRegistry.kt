package com.example.source

object SourceRegistry {

    private val sources = mutableMapOf<String, MusicSource>()

    init {
        registerSource(YouTubeSource())
        registerSource(JioSaavnSource())
    }

    fun registerSource(source: MusicSource) {
        sources[source.sourceId.uppercase()] = source
    }

    fun getSource(sourceId: String): MusicSource? {
        return sources[sourceId.uppercase()]
    }

    fun getAllSources(): List<MusicSource> {
        return sources.values.toList()
    }

    fun getYouTubeSource(): YouTubeSource {
        return sources["YOUTUBE"] as? YouTubeSource ?: YouTubeSource()
    }

    fun getJioSaavnSource(): JioSaavnSource {
        return sources["JIOSAAVN"] as? JioSaavnSource ?: JioSaavnSource()
    }
}
