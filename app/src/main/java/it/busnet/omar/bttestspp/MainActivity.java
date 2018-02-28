package it.busnet.omar.bttestspp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "Bluetooth: ";
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothAdapter mBluetoothAdapter;
     //used to identify handler message
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private BluetoothDevice deviceSelected;

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //List<String> s = pairedDevicesString();
        //setListAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, s));

        checkBTState();

        final Button riconetti = (Button)findViewById(R.id.riconnetti);
        riconetti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    btSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mConnectedThread = null;
                List<String> s = pairedDevicesString();
                setListAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice, s));
            }
        });

    }

    private List<String> pairedDevicesString(){
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        List<String> s = new ArrayList<String>();
        for(BluetoothDevice bt : pairedDevices)
            s.add(bt.getName());

        return s;
    }


    private void setListAdapter(ArrayAdapter<String> adpt){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Seleziona il dispositivo assiciato");

        final ArrayAdapter<String> arrayAdapter = adpt;
        builderSingle.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);

                BluetoothDevice device = findDevices(strName);
                deviceSelected = device;
                connect(device);
            }
        });
        builderSingle.show();
    }


    private BluetoothDevice findDevices(String s){
        for(BluetoothDevice bt : pairedDevices){
            if(bt.getName().equals(s)){
                return bt;
            }
        }
        return null;
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

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    public void connect(BluetoothDevice device){
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();


        if(btSocket == null || !btSocket.isConnected()) {

            if (deviceSelected != null) {
                connect(deviceSelected);
            } else {
                deviceSelected = null;
                List<String> s = pairedDevicesString();
                setListAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, s));
            }
        }else {
            Toast.makeText(getApplicationContext(), "Connesso.",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(mBluetoothAdapter==null) {
            Toast.makeText(getBaseContext(), "Questo dispositivo non supporta il Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (mBluetoothAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    private String decrypt(String message){
        String fist = "$destination=";
        String end = ";navigate@";
        if(message.contains(fist)){
            if(message.contains(end)){
                return message.substring(message.indexOf(fist) + fist.length() , message.indexOf(end));
            }
        }
        return "";
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            StringBuilder stringaletta = new StringBuilder();
            boolean leggi = false;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler

                    Log.d("Carattere letto: ", readMessage);
                    Log.d("Stringa: ", stringaletta.toString());

                    if(readMessage.equals("$")){
                        leggi = true;
                    }else if(readMessage.equals("@")){
                        stringaletta.append(readMessage);
                        Log.d("Stringa: ", stringaletta.toString());
                        leggi = false;
                        stringaletta = new StringBuilder();
                        getCoords(stringaletta.toString());
                    }

                    if(leggi){
                        stringaletta.append(readMessage);
                    }

                    //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        private void getCoords(String stringa){
            Log.d("CONTROLLA", stringa);
            if(stringa.length() > 0) {
                String coords = decrypt(stringa);
                if (coords != null && !coords.equals("")) {
                    Log.d("CONTROLLA", "Ok Apri navigatore");
                    openNavigation(coords);
                }else {
                    Log.d("CONTROLLA", "Stringa non valida");
                }
            }else {
                Log.d("CONTROLLA", "Stringa non valida");
            }
        }


        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }



}
