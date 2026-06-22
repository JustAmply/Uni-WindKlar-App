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
        override val title: String = "Info"
    }

    data class Detail(val parkId: String) : Route {
        override val title: String = "Windpark"
    }

    data class RegionDetail(val type: String, val id: String) : Route {
        override val title: String = when (type) {
            "city" -> "Gemeinde"
            "district" -> "Landkreis"
            "state" -> "Bundesland"
            else -> "Region"
        }
    }
}

val topLevelRoutes: List<Route> = listOf(
    Route.Map,
    Route.Stats,
    Route.Favorites,
    Route.Faq,
    Route.Profile,
)
