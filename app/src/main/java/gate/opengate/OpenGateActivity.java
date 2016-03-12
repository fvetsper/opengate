package gate.opengate;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;

public class OpenGateActivity extends Activity {

    private static final String SECRET = "12345";
    private OpenGateService mBoundService;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((OpenGateService.LocalBinder)service).getService();
            mBoundService.startListen();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        Intent intent = new Intent(this, OpenGateService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        Log.i("onStart", "entering");
        super.onStart();
        Intent incomingIntent = getIntent();
        TextView view = (TextView)findViewById(R.id.message);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(incomingIntent.getAction())) {
            Parcelable[] rawMsg = incomingIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsg != null && rawMsg.length == 1) {
                NdefMessage msg = (NdefMessage) rawMsg[0];
                NdefRecord[] records = msg.getRecords();
                if(records != null) {
                    String key = parseRecord(records[0]);
                    Log.i("onStart", key);
                    if(!key.equals(SECRET)){
                        view.setText("The device are not recognized! Please use the orignal Tag.");
                        return;
                    }
                }

            }
            view.setText("The device are recognized! We are ready.");
            Intent intent = new Intent(this, OpenGateService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private String parseRecord(NdefRecord record) {
        String text = null;
        try {
            byte[] payload = record.getPayload();
            String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0077;
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (Exception e) {

        }
        return text;
    }
}
