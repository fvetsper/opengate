package gate.opengate;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

/**
 * Created by felix on 18/10/2016.
 */
public class OpenGateGeofencingRegisterer implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private List<Geofence> geofencesToAdd;

    public OpenGateGeofencingRegisterer(Context context){
        mContext = context;
    }

    public void registerGeofences(List<Geofence> geofences) {

        geofencesToAdd = geofences;

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {


        if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final PendingIntent requestPendingIntent = createRequestPendingIntent();

        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(), requestPendingIntent);
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        for (Geofence geofence : geofencesToAdd) {
            builder.addGeofence(geofence);
        }
        return builder.build();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private PendingIntent createRequestPendingIntent() {
        Intent intent = new Intent(mContext, OpenGateGeofencingReceiver.class);
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
