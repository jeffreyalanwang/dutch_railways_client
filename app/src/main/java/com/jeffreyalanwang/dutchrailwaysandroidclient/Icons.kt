package com.jeffreyalanwang.dutchrailwaysandroidclient

object AppIcons {
    fun Trainset(trainset: Trainset) = when (trainset.quality) {
        TrainsetQuality.OLD -> R.drawable.ic_dr_traintype_slow
        TrainsetQuality.NEW -> R.drawable.ic_dr_traintype_fast
    }

    fun Amenity(amenity: TrainAmenity) = when(amenity) {
        TrainAmenity.STROOM ->
            R.drawable.ic_power
        TrainAmenity.TOILET ->
            R.drawable.ic_bathroom
        TrainAmenity.WIFI ->
            R.drawable.ic_wifi
        TrainAmenity.STILTE ->
            R.drawable.ic_quiet
        TrainAmenity.FIETS ->
            R.drawable.ic_bicycle
        TrainAmenity.TOEGANKELIJK ->
            R.drawable.ic_accessible
        TrainAmenity.unknown ->
            R.drawable.ic_more_hz
    }
}