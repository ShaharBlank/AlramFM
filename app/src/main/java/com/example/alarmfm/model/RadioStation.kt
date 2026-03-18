package com.example.alarmfm.model

data class RadioStation(
    val id: String,
    val nameHebrew: String,
    val nameEnglish: String,
    val streamUrl: String
)

object RadioStations {
    val list = listOf(
        RadioStation(
            "galgalatz", "גלגלץ", "Galgalatz",
            "https://glzwizzlv.bynetcdn.com/glglz_mp3"
        ),
        RadioStation(
            "galei_tzahal", "גלי צה\"ל", "Galei Tzahal",
            "https://glzwizzlv.bynetcdn.com/glz_mp3"
        ),
        RadioStation(
            "103fm", "103FM", "103 FM",
            "https://cdn.cybercdn.live/103FM/Live/playlist.m3u8"
        ),
        RadioStation(
            "galey_israel", "גלי ישראל", "Galey Israel 94FM",
            "http://gly-audioswitch1.ecast.co.il:9000/live"
        ),
        RadioStation(
            "radius_100fm", "רדיוס 100FM", "Radius 100FM",
            "https://cdn.cybercdn.live/Radios_100FM/Audio/icecast.audio"
        ),
        RadioStation(
            "kan88", "כאן 88", "Kan 88",
            "https://27863.live.streamtheworld.com/KAN_88.mp3?dist=radiohead"
        ),
        RadioStation(
            "reshet_bet", "רשת ב", "Kan Bet",
            "https://27913.live.streamtheworld.com/KAN_BET.mp3?dist=radiohead"
        ),
        RadioStation(
            "reshet_gimel", "רשת ג", "Kan Gimel",
            "https://19983.live.streamtheworld.com/KAN_GIMMEL.mp3?dist=radiohead"
        ),
        RadioStation(
            "102fm", "רדיו תל אביב", "Radio Tel Aviv 102FM",
            "https://102.livecdn.biz/102fm_aac"
        ),
        RadioStation(
            "eco99", "אקו 99FM", "Eco 99FM",
            "http://99.livecdn.biz/99fm"
        ),
        RadioStation(
            "radio_haifa", "רדיו חיפה", "Radio Haifa",
            "https://radiohaifa.co.il/stream"
        )
    )

    fun findById(id: String): RadioStation = list.find { it.id == id } ?: list.first()
}
