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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


/**
 * Created by Felix Vetsper on 25/2/2016.
 */
public class OpenGateService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private static final double MY_HOME_LATITUDE = 31.281283;
    private static final double MY_HOME_LONGITUDE = 34.798639;
    private static final float MY_HOME_RADIUS = 500;
    private static final String MY_OPEN_GATE_PHONE_NUMBER = "tel:0543909269";

    //Intent Action
    private static final String ACTION_FILTER = "gate.opengate.OpenGateService";

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("onReceive","enter broadcast receiver");
            String key = LocationManager.KEY_PROXIMITY_ENTERING;
            boolean enteringArea = intent.getBooleanExtra(key, false);
            if (enteringArea) {
                callNumber(MY_OPEN_GATE_PHONE_NUMBER);
            } else {
                stopListen();
            }
        }
    };

    private LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.i("onLocationChanged", "entering");
            Location homeLocation = new Location("");
            homeLocation.setLatitude(MY_HOME_LATITUDE);
            homeLocation.setLongitude(MY_HOME_LONGITUDE);
            float distance = location.distanceTo(homeLocation);
            Log.i("onLocationChanged", String.valueOf(distance));
            if (distance <= MY_HOME_RADIUS) {
                callNumber(MY_OPEN_GATE_PHONE_NUMBER);
            } else {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                localBroadcastManager.registerReceiver(receiver, new IntentFilter(ACTION_FILTER));

                Intent intent = new Intent(ACTION_FILTER);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                lm.addProximityAlert(MY_HOME_LATITUDE, MY_HOME_LONGITUDE, MY_HOME_RADIUS, -1, pendingIntent);
                Log.i("onLocationChanged","add Proximity Alert");
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


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        OpenGateService getService() {
            return OpenGateService.this;
        }
    }

    public void startListen() {
        Log.i("OpenGateService", "start listen");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null);
    }

    private void stopListen() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void callNumber(String number) {
        Log.i("callNumber", number);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse(number));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CALL_PHONE);
        Log.d("permissionCheck", "" + permissionCheck);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startActivity(callIntent);
        }
    }
}
