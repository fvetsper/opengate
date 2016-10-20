package gate.opengate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Created by felix on 18/10/2016.
 */
public class OpenGateCaller {

    private Context mContext;

    public OpenGateCaller(Context context) {
        mContext = context;
    }

    void callNumber(String number) {
        Log.i("callNumber", number);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int permissionCheck = ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.CALL_PHONE);
        Log.d("permissionCheck", "" + permissionCheck);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mContext.startActivity(callIntent);
        }
    }

}
