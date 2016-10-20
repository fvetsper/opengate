package gate.opengate;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import static gate.opengate.Constants.OPEN_GATE_IDENTIFIER;
import static gate.opengate.Constants.PHONE_NUMBER_KEY;

/**
 * Created by felix on 18/10/2016.
 */
public class OpenGateGeofencingReceiver extends IntentService {

    private OpenGateCaller mOpenGateCaller;


    public OpenGateGeofencingReceiver() {
        super("OpenGateGeofencingReceiver");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("onReceive", "enter broadcast receiver");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            String myOpenGatePhoneNumber = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(PHONE_NUMBER_KEY, null);
            mOpenGateCaller.callNumber(myOpenGatePhoneNumber);
        }
    }
}
