package com.example.walkingnavigator


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.walkingnavigator.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.Error
import kotlin.math.sqrt


const val PERMISSION_REQUEST_LOCATION = 0
const val APP_PREFERENCES = "mysettings"
const val PREFERENCES_FIRST_REQUEST = "request"
const val REFERENCES_RECORD_STEPS = "record"
private var firstRequest: Boolean = true
private var latitudeUser: Double = 0.00
private var longitudeUser: Double = 0.00
private var latitudeFinish: Double = 0.00
private var longitudeFinish: Double = 0.00
private var recordSteps: Int = 0

class MainActivity : AppCompatActivity() {
    private var countSteps: Int = 0
    private val mSettings: SharedPreferences by lazy {
        getSharedPreferences(
            APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
    }
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val sensor: Sensor? by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mapView: MapView by lazy { binding.mapview }
    private val locationManager: android.location.LocationManager by lazy {
        getSystemService(
            LOCATION_SERVICE
        ) as android.location.LocationManager
    }
    private val locationListener: android.location.LocationListener =
        android.location.LocationListener { p0 ->
            latitudeUser = p0.latitude
            longitudeUser = p0.longitude
        }
    private val sListener = object : SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            val value = p0!!.values
            val x = value[0]
            val y = value[1]
            val z = value[2]
            val absoluteValue = sqrt(x * x + y * y + z * z)
            if ((absoluteValue > 12.0f) && (absoluteValue < 14.0f)) countSteps++
            binding.tvStep.text = "Шаги: " + (countSteps / 2).toString()
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }

    }
    private val drivingRouter: DrivingRouter by lazy {
        DirectionsFactory.getInstance().createDrivingRouter()
    }
    private lateinit var routesCollection: MapObjectCollection
    private lateinit var drivingSession: DrivingSession
    private val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
            drivingRoutes.forEachIndexed { index, route ->
                routesCollection.addPolyline(route.geometry).apply {
                    zIndex = 5f
                    setStrokeColor(Color.BLACK)
                    strokeWidth = 3f
                    outlineColor = Color.BLUE
                    outlineWidth = 1f
                }
            }
        }

        override fun onDrivingRoutesError(p0: Error) {

        }


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(this)
        setContentView(binding.root)
        (application as App).setMap(mapView.mapWindow.map)
        loadPreferences()
        if (checkSelfPermissionCompat(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
        } else {
            requestLocationPermission()
        }
        MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow).isVisible = true
        val arguments = intent.extras
        if (arguments != null) {
            val text = arguments.getString("text") ?: ""
            binding.tvFinish.text = "Финишь: " + text
            latitudeFinish = arguments.getDouble("latitude")
            longitudeFinish = arguments.getDouble("longitude")
        }

        binding.btnStart.setOnClickListener {
            binding.tvFinish.isVisible = false
            routesCollection = mapView.mapWindow.map.mapObjects.addCollection()
            createRouter(latitudeFinish, longitudeFinish)
            sensorManager.registerListener(sListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        binding.btnLocationUser.setOnClickListener {
            mapView.mapWindow.map.move(
                CameraPosition(
                    Point(latitudeUser, longitudeUser),
                    17.0f,
                    0.0f,
                    0.0f
                )
            )
        }

        binding.btnStop.setOnClickListener {
            val myDialogFragment = MyDialogFragment(countSteps / 2)
            myDialogFragment.show(supportFragmentManager, "tag")
        }

        binding.tvFinish.setOnClickListener {
            goToSearchActivity()
        }

    }

    fun clearMap() {
        binding.tvFinish.isVisible = true
        if (this::routesCollection.isInitialized) routesCollection.clear()
        binding.tvStep.text = ""
        if (countSteps / 2 > recordSteps) {
            recordSteps = countSteps / 2
            binding.tvRecord.text = "Рекорд: $recordSteps  шагов"
        }
        countSteps = 0
        sensorManager.unregisterListener(sListener, sensor)
    }

    private fun createRouter(latitude: Double, longitude: Double) {
        val drivingOptions = DrivingOptions().apply {
            routesCount = 3
        }
        val points = buildList {
            add(
                RequestPoint(
                    Point(latitudeUser, longitudeUser),
                    RequestPointType.WAYPOINT,
                    null,
                    null
                )
            )
            add(RequestPoint(Point(latitude, longitude), RequestPointType.WAYPOINT, null, null))
        }
        drivingSession = drivingRouter.requestRoutes(
            points,
            drivingOptions,
            VehicleOptions(),
            drivingRouteListener
        )
    }

    private fun savePreferences() {
        val editor = mSettings.edit()
        editor.putBoolean(PREFERENCES_FIRST_REQUEST, firstRequest)
        editor.putInt(REFERENCES_RECORD_STEPS, recordSteps)
        editor.apply()
    }

    private fun loadPreferences() {
        if (mSettings.contains(REFERENCES_RECORD_STEPS)) {
            recordSteps = mSettings.getInt(REFERENCES_RECORD_STEPS, 0)
            binding.tvRecord.text =
                "Рекорд: $recordSteps  шагов"
            firstRequest = mSettings.getBoolean(PREFERENCES_FIRST_REQUEST, true)
        }
    }

    private fun goToSearchActivity() {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationManager.requestLocationUpdates(
            android.location.LocationManager.GPS_PROVIDER,
            0, 0.0F, locationListener
        )
        locationManager.requestLocationUpdates(
            android.location.LocationManager.NETWORK_PROVIDER,
            0, 0.0F, locationListener
        )

    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
        savePreferences()

    }


    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissionsCompat(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_LOCATION
            )
        } else {
            if (firstRequest) {
                requestPermissionsCompat(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_LOCATION
                )
                firstRequest = false
            } else {
                Toast.makeText(
                    this,
                    "Разрешение не может быть запрошено, перейдите в настройки",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение предоставлено", Toast.LENGTH_LONG).show()
                getLocation()
            } else {
                Toast.makeText(this, "Разрешение не предоставлено", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun AppCompatActivity.requestPermissionsCompat(
        permissionsArray: Array<String>,
        requestCode: Int
    ) {
        ActivityCompat.requestPermissions(this, permissionsArray, requestCode)
    }

    private fun AppCompatActivity.checkSelfPermissionCompat(permission: String) =
        ActivityCompat.checkSelfPermission(this, permission)

    private fun AppCompatActivity.shouldShowRequestPermissionRationaleCompat(permission: String) =
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}


