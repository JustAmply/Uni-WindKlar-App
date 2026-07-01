# SQLDelight domain target model

WindKlar's local model will use `wind_turbine` for atomic MaStR installation master data, `wind_park` for precomputed citizen-facing aggregates, `metric` for production and acceptance impact values, `favorite_wind_park` for saved parks, `recent_wind_park` for recently opened parks, `data_hint` for local data-quality hints, and optional `snapshot_metadata` for source and calculation assumptions. This aligns the database with the product language and avoids overloading existing production/search-history tables with different concepts.
