package app.navigation

sealed interface Route {
    val title: String

    data object Start : Route {
        override val title: String = "Start"
    }

    data object Map : Route {
        override val title: String = "Karte"
    }

    data object Stats : Route {
        override val title: String = "Statistiken"
    }

    data object Favorites : Route {
        override val title: String = "Favoriten"
    }

    data object Faq : Route {
        override val title: String = "FAQ"
    }

    data object Profile : Route {
        override val title: String = "Einstellungen"
    }

    data class Detail(val parkId: String) : Route {
        override val title: String = "Windpark"
    }
}

val topLevelRoutes: List<Route> = listOf(
    Route.Map,
    Route.Stats,
    Route.Favorites,
    Route.Faq,
    Route.Profile,
)
