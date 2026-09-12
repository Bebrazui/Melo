package com.melo.desktop.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(val title: String, val icon: ImageVector) {
    HOME("Главная", Icons.Rounded.Home),
    SEARCH("Поиск", Icons.Rounded.Search),
    FAVORITES("Любимые треки", Icons.Rounded.Favorite),
    PLAYLISTS("Плейлисты", Icons.AutoMirrored.Rounded.QueueMusic),
    HISTORY("История", Icons.Rounded.History),
    SETTINGS("Настройки", Icons.Rounded.Settings),
    PROFILE("Профиль", Icons.Rounded.Person),
}
