package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainsetQuality
import kotlin.reflect.KClass

object AppIcons {
    fun Trainset(trainset: Trainset?) = when (trainset?.quality) {
        TrainsetQuality.OLD -> R.drawable.ic_dr_traintype_slow
        TrainsetQuality.NEW -> R.drawable.ic_dr_traintype_fast
        null -> R.drawable.ic_dr_trainservice
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

    fun <T: Place> PlaceType(cls: KClass<T>)
        =    if(cls.java.isAssignableFrom(Station::class.java))
                R.drawable.ic_dr_station
        else if(cls.java.isAssignableFrom(Area::class.java))
                R.drawable.ic_dr_area
        else
                R.drawable.ic_dr_area // should be an error; if so, show something close enough
}