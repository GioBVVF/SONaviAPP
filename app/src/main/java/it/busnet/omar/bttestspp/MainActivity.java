package it.busnet.omar.bttestspp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {

    private BluetoothSPP bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bt = new BluetoothSPP(getApplicationContext());

        if (!bt.isBluetoothAvailable()) {
            return;
        }

        if (!bt.isBluetoothEnabled()) {
            bt.enable();
        }

        bt.setupService(); // setup bluetooth service
        bt.startService(false); // start bluetooth service

        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);

        Button send = (Button) findViewById(R.id.button);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bt.send("test", true);
            }
        });


        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                Log.d("RECEIVE", message);

                String fist = "$destination=";
                String end = ";navigate@";
                if(message.contains(fist)){
                    if(message.contains(end)){
                        String result = message.substring(message.indexOf(fist) + fist.length() , message.indexOf(end));
                        openNavigation(result);
                    }
                }
            }
        });

    }



    private void openNavigation(String cord){
        Intent mapIntent = new Intent();
        // we want to view the map
        mapIntent.setAction(Intent.ACTION_VIEW);

        // this will be shown as primary marker in the map
        // the coordinate 53.2,8.8 is in north germany where the map is centered around
        // z=1 means zoomlevel=1 showing the continent
        // the marker's caption will be "primary marker"
        Uri uri = Uri.parse("geo:"+cord+"?q="+cord+"(Posizione)");
        mapIntent.setDataAndType(uri, null);

        // this is the maps Caption
        mapIntent.putExtra(Intent.EXTRA_TITLE, "Hello Map");

        // the map will contain 2 additional point of interest
        mapIntent.putExtra("de.k3b.POIS",
                "<poi ll='"+cord+"'/>\n" +
                        "");

        try {
            startActivityForResult(Intent.createChooser(mapIntent,"Choose app to show location"), 4711);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_ANDROID);

            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }
}
