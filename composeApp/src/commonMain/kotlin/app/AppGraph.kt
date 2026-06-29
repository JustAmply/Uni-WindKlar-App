package app

import app.core.location.LocationProvider
import app.data.local.source.SourceDatabase
import app.data.local.user.UserDatabase
import app.data.local.dao.*
import app.data.repository.SqlDelightWindParkRepository
import app.data.repository.WindParkRepository
import app.feature.detail.ParkDetailViewModel
import app.feature.detail.RegionDetailViewModel
import app.feature.favorites.FavoritesViewModel
import app.feature.map.MapViewModel
import app.feature.profile.ProfileViewModel
import app.feature.stats.StatsViewModel
import app.feature.start.StartViewModel

class AppGraph(
    sourceDatabase: SourceDatabase,
    userDatabase: UserDatabase,
    private val locationProvider: LocationProvider,
) {
    val repository: WindParkRepository = SqlDelightWindParkRepository(
        windParkDao = SqlDelightWindParkDao(sourceDatabase),
        windTurbineDao = SqlDelightWindTurbineDao(sourceDatabase),
        metricDao = SqlDelightMetricDao(sourceDatabase),
        favoriteDao = SqlDelightFavoriteDao(userDatabase),
        recentWindParkDao = SqlDelightRecentWindParkDao(userDatabase),
        dataHintDao = SqlDelightDataHintDao(userDatabase),
        snapshotMetadataDao = SqlDelightSnapshotMetadataDao(sourceDatabase),
        settingsDao = SqlDelightSettingsDao(userDatabase),
        summaryDao = SqlDelightSummaryDao(sourceDatabase)
    )

    fun mapViewModel(): MapViewModel =
        MapViewModel(repository, repository, locationProvider)

    fun favoritesViewModel(): FavoritesViewModel =
        FavoritesViewModel(repository)

    fun statsViewModel(): StatsViewModel =
        StatsViewModel(repository)

    fun profileViewModel(): ProfileViewModel =
        ProfileViewModel(repository)

    fun parkDetailViewModel(parkId: String): ParkDetailViewModel =
        ParkDetailViewModel(parkId, repository, repository)

    fun regionDetailViewModel(type: String, id: String): RegionDetailViewModel =
        RegionDetailViewModel(type, id, repository)

    fun startViewModel(): StartViewModel =
        StartViewModel(repository)
}
