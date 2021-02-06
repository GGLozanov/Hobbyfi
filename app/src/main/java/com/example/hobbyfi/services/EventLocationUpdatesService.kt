package com.example.hobbyfi.services

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.createNotificationChannel
import com.example.hobbyfi.shared.isServiceForegrounded
import com.example.hobbyfi.ui.chatroom.EventMapsActivity
import com.example.hobbyfi.utils.LocationUtils
import com.google.android.gms.location.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*


// adapted from Google location update sample: https://github.com/android/location-samples/blob/main/LocationUpdatesForegroundService/app/src/main/java/com/google/android/gms/location/sample/locationupdatesforegroundservice/LocationUpdatesService.java
class EventLocationUpdatesService : Service(), KodeinAware {
    override val kodein: Kodein by kodein(MainApplication.applicationContext)
    private val prefConfig: PrefConfig by instance(tag = "prefConfig")
    private val localBroadcastManager: LocalBroadcastManager by instance(tag = "localBroadcastManager")

    private var relatedEvent: Event? = null
    private var initialAuthUserGeoPoint: UserGeoPoint? = null

    private val binder: IBinder = LocalBinder()

    // local binder only for this given process; no use for IPC methods
    inner class LocalBinder : Binder() {
        val service: EventLocationUpdatesService
            get() = this@EventLocationUpdatesService
    }

    // verifies activity state depending on unbinding through configuration change OR actual foreground state change
    // relegates activation of service in cases where the activity is simply rotated or multi-window is enabled
    // i.e. config change
    private var isChangingConfiguration = false

    private lateinit var notificationManager: NotificationManager

    private var locationRequest: LocationRequest? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var locationCallback: LocationCallback? = null

    private lateinit var serviceHandler: Handler

    private var lastLocation: Location? = null

    @ExperimentalCoroutinesApi
    override fun onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }
        createLocationRequest()
        getLastLocation()
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // >= API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) run {
            createNotificationChannel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        if(intent == null) {
            Log.i(TAG, "Service intent == null. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val startedFromNotification = intent.getBooleanExtra(
            Constants.STARTED_UPDATE_LOCATION_FROM_NOTIFICATION,
            false
        )

        relatedEvent = intent.extras!![Constants.EVENT] as Event
        initialAuthUserGeoPoint = intent.extras!![Constants.USER_GEO_POINT] as UserGeoPoint

        // in the case that user decides to remove notification for location updates (triggered by Notification action button)
        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        // Called when a client (EventMapsActivity) comes to the foreground
        // and binds with this service => shouldn't be a foregound service and notification go brr
        Log.i(TAG, "Triggered onBind()")
        stopForeground(true) // reset any notifications
        isChangingConfiguration = false
        return binder
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "Triggered onRebind()")
        stopForeground(true) // reset any notifications
        isChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isChangingConfiguration = true
    }

    fun requestLocationUpdates(relatedEvent: Event, initialAuthUserGeoPoint: UserGeoPoint) {
        Log.i(TAG, "Requesting location updates")
        prefConfig.writeRequestingLocationUpdates(true)
        startService(
            Intent(
                applicationContext,
                EventLocationUpdatesService::class.java
            ).apply {
                putExtra(Constants.EVENT, relatedEvent)
                putExtra(Constants.USER_GEO_POINT, initialAuthUserGeoPoint)
            }
        ) // Start the FG service (called from Activity)
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback, Looper.myLooper()
            )
        } catch (ex: SecurityException) {
            prefConfig.writeRequestingLocationUpdates(false)
            Log.e(TAG, "Lost location permission. Could not request updates. $ex")
        }
    }

    fun removeLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            prefConfig.writeRequestingLocationUpdates(false)
            stopSelf()
        } catch (ex: SecurityException) {
            prefConfig.writeRequestingLocationUpdates(true)
            Log.e(TAG, "Lost location permission. Could not remove updates. $ex")
        }
    }

    @ExperimentalCoroutinesApi
    override fun onUnbind(intent: Intent): Boolean {
        Log.i(
            TAG,
            "Last client unbound from service. Is changing config: ${isChangingConfiguration}"
        )

        // Called when the last client (EventMapsActivity) unbinds from this
        // service. If this method is called due to a configuration change in EventMapsActivity,
        // do nothing. Otherwise, make this service a foreground service.
        if((prefConfig.readRequestLocationServiceRunning() || !isChangingConfiguration)
                && prefConfig.readRequestingLocationUpdates()) {
            Log.i(TAG, "Starting foreground service")

            startForeground(SERVICE_NOTIFICATION_ID, buildNotification())
        }

        return true // ensures onRebind() is called upon client coming back
    }

    override fun onDestroy() {
        serviceHandler.removeCallbacksAndMessages(null) // unregister handler from receiving any more messages
        super.onDestroy()
    }

    // Notification builder method required for foreground services after Android 8 (Oreo)
    @ExperimentalCoroutinesApi
    private fun buildNotification(): Notification {
        val intent = Intent(this, EventLocationUpdatesService::class.java).apply {
            putExtra(Constants.STARTED_UPDATE_LOCATION_FROM_NOTIFICATION, true)
            putExtra(Constants.EVENT, relatedEvent)
            putExtra(Constants.USER_GEO_POINT, initialAuthUserGeoPoint)
        }

        val text: String = LocationUtils.getLocationText(lastLocation)

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        val servicePendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // The PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, EventMapsActivity::class.java).apply {
                putExtra(Constants.EVENT, relatedEvent)
                putExtra(Constants.USER_GEO_POINT, initialAuthUserGeoPoint)
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(
            this,
            resources.getString(com.example.hobbyfi.R.string.default_notification_channel_id)
        ).apply {
            addAction(
                R.drawable.ic_baseline_launch_24, getString(R.string.launch_activityG),
                activityPendingIntent
            )
            addAction(
                R.drawable.ic_baseline_cancel_24, getString(R.string.remove_location_updates),
                servicePendingIntent
            )
            color = ContextCompat.getColor(
                this@EventLocationUpdatesService,
                R.color.colorBackground
            )
            setSmallIcon(applicationInfo.icon)
            setContentText(text)
            setContentTitle(LocationUtils.getLocationTitle(applicationContext))
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_HIGH
            setTicker(text)
            setWhen(System.currentTimeMillis())

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(resources.getString(com.example.hobbyfi.R.string.default_notification_channel_id))
            }
        }.build()
    }

    private fun getLastLocation() {
        try {
            fusedLocationProviderClient.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        lastLocation = task.result
                    } else {
                        Log.w(TAG, "Failed to get location.")
                    }
                }
        } catch (ex: SecurityException) {
            Log.e(TAG, "Lost location permission. $ex")
        }
    }

    @ExperimentalCoroutinesApi
    private fun onNewLocation(location: Location) {
        Log.i(TAG, "New location: $location")
        lastLocation = location

        // Notify anyone listening for broadcasts about the new location.
        val intent = Intent(Constants.UPDATED_LOCATION_ACTION).apply {
            putExtra(Constants.UPDATED_LOCATION, location)
        }
        localBroadcastManager.sendBroadcast(intent)

        // Update notification content if running as a foreground service.
        if(isServiceForegrounded(this::class.java)) {
            notificationManager.notify(SERVICE_NOTIFICATION_ID, buildNotification())
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    companion object {
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 50000

        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2

        private const val SERVICE_NOTIFICATION_ID = 12345678

        private val TAG = EventLocationUpdatesService::class.simpleName
    }
}