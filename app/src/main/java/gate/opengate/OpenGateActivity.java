package gate.opengate;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static gate.opengate.Constants.*;

public class OpenGateActivity extends Activity {


    private OpenGateService mBoundService;
    private ServiceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View v = super.getView(position, convertView, parent);
                if (position == getCount()) {
                    ((TextView)v.findViewById(android.R.id.text1)).setText("");
                    ((TextView)v.findViewById(android.R.id.text1)).setHint(getItem(getCount())); //"Hint to be displayed"
                }

                return v;
            }

            @Override
            public int getCount() {
                return super.getCount()-1; // you don't display last item. It is used as hint.
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add("100 meter");
        adapter.add("200 meter");
        adapter.add("300 meter");
        adapter.add("400 meter");
        adapter.add("Region Distance");
        Spinner spinner = (Spinner) findViewById(R.id.radius);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getCount());

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.GRAY);
        }
    }

    @Override
    public void onStart() {
        Log.i("onStart", "entering");
        super.onStart();
        setPreferences();
        Intent incomingIntent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(incomingIntent.getAction())) {
            Parcelable[] rawMsg = incomingIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsg != null && rawMsg.length == 1) {
                NdefMessage msg = (NdefMessage) rawMsg[0];
                NdefRecord[] records = msg.getRecords();
                if(records != null) {
                    String key = parseRecord(records[0]);
                    if(!key.equals(SECRET)){
                        return;
                    }
                }

            }

            mConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mBoundService = ((OpenGateService.LocalBinder)service).getService();
                    mBoundService.startListen();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            Intent serviceIntent = new Intent(this, OpenGateService.class);
            startService(serviceIntent);
            bindService(serviceIntent, mConnection, 0);
        }
    }

    private void setPreferences() {
        String gatePhoneNumber = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(PHONE_NUMBER_KEY, null);
        if(gatePhoneNumber != null) {
            EditText phoneEditText = (EditText) findViewById(R.id.phone);
            phoneEditText.getText().clear();
            phoneEditText.getText().append(gatePhoneNumber);
        }
        String radius = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(RADIUS_KEY, null);
        if (radius != null) {
            Spinner spinner = (Spinner) findViewById(R.id.radius);
            spinner.setSelection(Integer.parseInt(radius) / 100 - 1);

        }
        String address = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).getString(ADDRESS_KEY, null);
        if (address != null) {
            EditText locationEditText = (EditText) findViewById(R.id.location);
            locationEditText.getText().clear();
            locationEditText.getText().append(address);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        if(mConnection != null) {
            unbindService(mConnection);
        }
    }

    public void OnUpdateClicked(View view) {
        Log.i("OnUpdateClicked","update");
        EditText phoneEditText = (EditText) findViewById(R.id.phone);
        String phoneNumber = phoneEditText.getText().toString();
        EditText locationEditText = (EditText) findViewById(R.id.location);
        SharedPreferences.Editor editor = getSharedPreferences(OPEN_GATE_IDENTIFIER, MODE_PRIVATE).edit();

        String address = locationEditText.getText().toString();
        editor.putString(ADDRESS_KEY, address);
        Address location = getLocationFromAddress(address);
        Spinner radiusSpinner = (Spinner)findViewById(R.id.radius);
        String radius = radiusSpinner.getSelectedItem().toString();
        radius = radius.substring(0, radius.indexOf("meter") - 1);
        Log.i("radius", radius);
        Log.i("phoneNumber", phoneNumber);


        editor.putString(PHONE_NUMBER_KEY, phoneNumber);
        if(location != null) {
            Log.i("location", String.valueOf(location.getLatitude()));
            editor.putString(ADDRESS_LATITUDE_KEY, Double.toString(location.getLatitude()));
            editor.putString(ADDRESS_LONGITUDE_KEY, Double.toString(location.getLongitude()));
        }
        editor.putString(RADIUS_KEY, radius);

        editor.commit();
    }

    private String parseRecord(NdefRecord record) {
        String text = null;
        try {
            byte[] payload = record.getPayload();
            String textEncoding = ((payload[0] & 0200) == 0) ? StandardCharsets.UTF_8.name() : StandardCharsets.UTF_16.name();
            int languageCodeLength = payload[0] & 0077;
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (Exception e) {

        }
        return text;
    }

    public Address getLocationFromAddress(String strAddress){
        Address address = null;
        Geocoder coder = new Geocoder(this);
        try {
            List<Address> addressList = coder.getFromLocationName(strAddress, 1);
            if (addressList.isEmpty() == false) {
                address = addressList.get(0);
            }
        } catch (IOException e) {
            Log.e("getLocationFromAddress", "fail to get location from address", e);
        }
        return address;
    }
}
