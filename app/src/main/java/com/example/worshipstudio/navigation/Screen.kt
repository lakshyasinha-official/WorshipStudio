package com.example.worshipstudio.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SongList : Screen("song_list")
    object AddSong : Screen("add_song?songId={songId}") {
        fun createRoute(songId: String? = null) =
            if (songId != null) "add_song?songId=$songId" else "add_song"
    }
    object SongDetail : Screen("song_detail/{songId}") {
        fun createRoute(songId: String) = "song_detail/$songId"
    }
    object CreateSet : Screen("create_set")
    object SetDetail : Screen("set_detail/{setId}") {
        fun createRoute(setId: String) = "set_detail/$setId"
    }
    object LiveSession : Screen("live_session/{sessionId}/{isAdmin}") {
        fun createRoute(sessionId: String, isAdmin: Boolean) = "live_session/$sessionId/$isAdmin"
    }
}
