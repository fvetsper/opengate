package gate.opengate;


import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import static gate.opengate.Constants.ADDRESS_LATITUDE_KEY;
import static gate.opengate.Constants.ADDRESS_LONGITUDE_KEY;
import static gate.opengate.Constants.OPEN_GATE_IDENTIFIER;
import static gate.opengate.Constants.PHONE_NUMBER_KEY;
import static gate.opengate.Constants.RADIUS_KEY;


/**
 * Created by Felix Vetsper on 25/2/2016.
 */
public class OpenGateService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private static final float MY_HOME_RADIUS = 50;
    private static final int ONE_HOUR = 1000 * 60 * 60;
    private static final int ONE_MINUTE = 1000 * 60;

    private PendingIntent mGeofencePendingIntent;
    private Geofence mGeofence;
    private GoogleApiClient mGoogleApiClient;
    private WakeLock wakeLock;
    private GoogleApiClient.ConnectionCallbacks connectionListner;
    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener;
    private BroadcastReceiver receiver;

    //Intent Action
    private static final String ACTION_FILTER = "gate.opengate.OpenGateService";
    private boolean connected;


    private void removeProximityAlert() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        unregisterReceiver(receiver);
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent());
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent == null) {
            Intent intent = new Intent(ACTION_FILTER);
            mGeofencePendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mGeofencePendingIntent;
    }

    private Geofence createGeofence() {
        if (mGeofence == null) {
            double myHomeLatitude = Double.valueOf(
                    getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(ADDRESS_LATITUDE_KEY, null));
            double myHomeLongitude = Double.valueOf(
                    getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(ADDRESS_LONGITUDE_KEY, null));
            float myHomeGeofenceRaduis = Float.valueOf(
                    getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(RADIUS_KEY, null));
            mGeofence = new Geofence.Builder()
                    .setExpirationDuration(ONE_HOUR)
                    .setRequestId(OPEN_GATE_IDENTIFIER)
                    .setCircularRegion(myHomeLatitude, myHomeLongitude, myHomeGeofenceRaduis)
                    .setNotificationResponsiveness(ONE_MINUTE / 15)
                    .setLoiteringDelay(ONE_MINUTE / 15)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                    .build();
        }
        return mGeofence;
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.addGeofence(createGeofence());
        return builder.build();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.i("onLocationChanged", "entering");
            Location homeLocation = new Location("");
            double myHomeLatitude = Double.valueOf(
                    getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(ADDRESS_LATITUDE_KEY, null));
            double myHomeLongitude = Double.valueOf(
                    getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(ADDRESS_LONGITUDE_KEY, null));
            String myOpenGatePhoneNumber = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(PHONE_NUMBER_KEY, null);
            homeLocation.setLatitude(myHomeLatitude);
            homeLocation.setLongitude(myHomeLongitude);
            float distance = location.distanceTo(homeLocation);
            Log.i("onLocationChanged", String.valueOf(distance));
            if (distance <= MY_HOME_RADIUS) {
                mGoogleApiClient.disconnect();
                callNumber(myOpenGatePhoneNumber);
                stopSelf();
            } else {
                setProximityAlert();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void setProximityAlert() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("onReceive", "enter broadcast receiver");
                GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
                int geofenceTransition = geofencingEvent.getGeofenceTransition();
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                        || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                    String myOpenGatePhoneNumber = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(PHONE_NUMBER_KEY, null);
                    removeProximityAlert();
                    mGoogleApiClient.disconnect();
                    callNumber(myOpenGatePhoneNumber);
                    stopSelf();
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(ACTION_FILTER));

        PendingIntent pendingIntent = getGeofencePendingIntent();

        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                pendingIntent);

        Log.i("onLocationChanged", "add Proximity Alert");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onDestroy() {
        if(connectionListner != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(connectionListner);
        }
        if(connectionFailedListener != null) {
            mGoogleApiClient.unregisterConnectionFailedListener(connectionFailedListener);
        }
        if(wakeLock != null) {
            wakeLock.release();
        }
        Log.i("onDestroy", "service destroyed");
    }

    /**
     @Override protected void onHandleIntent(Intent intent) {
     GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
     int geofenceTransition = geofencingEvent.getGeofenceTransition();
     if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
     requestSingleUpdate();
     }
     }
     **/

    public class LocalBinder extends Binder {
        OpenGateService getService() {
            return OpenGateService.this;
        }
    }

    public void startListen() {
        connected = false;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GateWakelock");
        if(wakeLock != null && wakeLock.isHeld() == false) {
            wakeLock.acquire();
        }

        connectionListner = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                if (!connected) {
                    Log.i("startListen", "connected");
                    connected = true;
                    requestSingleUpdate();
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
            }
        };

        connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.i("onConnectionFailed", "" + " " + connectionResult.getErrorCode() + connectionResult.getErrorMessage());
            }
        };


        mGoogleApiClient.registerConnectionCallbacks(connectionListner);
        mGoogleApiClient.registerConnectionFailedListener(connectionFailedListener);
        if(mGoogleApiClient.isConnected() == false) {
            Log.i("startListen", "connecting");
            mGoogleApiClient.connect();
        }
    }

    private void requestSingleUpdate() {
        Log.i("OpenGateService", "requestSingleUpdate");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
    }
}
