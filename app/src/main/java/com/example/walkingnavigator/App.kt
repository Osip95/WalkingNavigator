package com.example.walkingnavigator

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import  com.yandex.mapkit.map.Map

const val MAPKIT_API_KEY = "7d806435-9cd5-4eac-8f62-e4c29b11fe3c"

class App : Application() {

    private lateinit var map: Map
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(MAPKIT_API_KEY)
    }

    fun getMap(): Map {
        return map
    }

    fun setMap(map: Map) {
        this.map = map
    }
}