package com.example.copiedbtcode;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.IOException;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";
    private TextView textView;
    //Bluetooth parameters declare
    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;
    Button btnONOFF;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    long t = System.currentTimeMillis();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;

    //SQL server parameter declare
    //private static String ip="192.168.1.168";
    private static String ip="192.168.0.102";
    private static String port = "1433";
    private static String Classes = "net.sourceforge.jtds.jdbc.Driver";
    private static String database = "testData";
    private static String username = "test";
    //private static String password = "test";
    private static String password = "";
    //private static String url = "jdbc:jtds:sqlserver://"+ip+":"+port+"/"+database;
    private static String url="jdbc:jtds:sqlserver://192.168.0.102:1433;database=testData;integratedSecurity=true";
    private Connection connection = null;

    //gyroscope
    private SensorManager sensormanager;
    private HandlerThread gyroscopeThread;
    private Handler gyroscopeHandler;
    private HandlerThread gpsThread;
    private Handler gpsHandler;
    private HandlerThread bluetoothThread;
    private Handler bluetoothHandler;

    //private File textfile;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private long GyroscopeLastWritingTime=0;
    private ArrayList<Float> GyroscopeValue = new ArrayList<>();

    private long GpsLastWritingTime=0;
    private double lat=0; //gps latitude
    private double lon=0;//gps longitude

    private int rssi;//bluetooth signal strength



    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            File file = new File(MainActivity.this.getFilesDir(), "text");
            if (!file.exists()) {
                file.mkdir();
            }

            //FirebaseDatabase database= FirebaseDatabase.getInstance();
            //final DatabaseReference mRef = database.getReference("simCoder");
            Log.d(TAG, "onReceive: ACTION FOUND.");
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);      //get the rssi signal strength
                Toast.makeText(getApplicationContext(), "  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show(); //show the rssi signal strength once
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());

                Date nowforbluetooth = new Date();
                long Bluetoothtimestamp = nowforbluetooth.getTime();
                String textToSave = sdf.format(Bluetoothtimestamp)+" bluetooth "+ device.getName() + " " + device.getAddress()+" "+rssi+"\r"+"\n";

                try {
                    File gpxfile = new File(file, "sample");
                    FileWriter writer = new FileWriter(gpxfile,true);
                    writer.append(textToSave);
                    writer.flush();
                    writer.close();
                    Log.d(TAG, "filesaved Locally");

                    //Firebase realtime database setting
                    //Firebase saving
                    //String childValue = textToSave;
                    //mRef.setValue(childValue);
                    //Log.d(TAG, "filesaved Database" );

                    // Saves a message to the Firebase Realtime Database but sanitizes the text by removing swearwords.

                } catch (IOException e) {
                    e.printStackTrace();
                }
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                try {
                    lvNewDevices.setAdapter(mDeviceListAdapter);
                } catch (Exception e) {
                }

                if (connection != null) {
                    Statement statement = null;
                    try {
                        statement = connection.createStatement();
                        //ResultSet resultSet = statement.executeQuery("Select * from TEST_TABLE;");

                        //ResultSet resultSet = statement.executeQuery("SELECT C1 FROM TEST_TABLE;");
                        Object resultSet = statement.executeUpdate("INSERT INTO TEST_TABLE" + " VALUES ('"+device.getName()+"','"+device.getAddress()+"')");


                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }

                if (connection != null) {
                    Statement statement = null;
                    try {
                        statement = connection.createStatement();
                        //ResultSet resultSet = statement.executeQuery("Select * from TEST_TABLE;");
                        //ResultSet resultSet = statement.executeQuery("SELECT C1 FROM TEST_TABLE;");
                       // ResultSet resultSet2 = statement.executeQuery("Select * from TEST_TABLE;");
                        ResultSet resultSet2 = statement.executeQuery("SELECT * FROM TEST_TABLE WHERE CONVERT(VARCHAR,C1)='hello';");


                        //                while (resultSet.next()){
//                    textView.setText(resultSet.getString(1));
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
                        while(resultSet2.next()) {
                            textView.setText(resultSet2.getString(1));
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.v(TAG, "Entered the Finished ");
                mBluetoothAdapter.startDiscovery();
            }



        }
    };


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);
        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
        btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        textView = findViewById(R.id.textView4);

   //     StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
   //     StrictMode.setThreadPolicy(policy);
   //     try {
   //         Class.forName(Classes);
   //         connection = DriverManager.getConnection(url, username,password);
   //         textView.setText("SUCCESS");
   //     } catch (ClassNotFoundException e) {
   //         e.printStackTrace();
   //         textView.setText("ERROR");
   //     } catch (SQLException e) {
   //         e.printStackTrace();
   //         textView.setText("FAILURE");
    //    }

    }

    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }


    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
            //check BT permissions in manifest
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            IntentFilter discoverDevicesIntent2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent2);

        }
        if (!mBluetoothAdapter.isDiscovering()) {
            //check BT permissions in manifest
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            IntentFilter discoverDevicesIntent2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent2);

        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }


    public void GET_STARTED(View view) {
        setContentView(R.layout.app_function);
    }

    public void APP_FUNCTION(View view) {
        setContentView(R.layout.your_data);
    }

    public void your_data(View view) {
        setContentView(R.layout.turn_on_bluetooth);
    }

    public void turn_on_bluetooth(View view) {
        setContentView(R.layout.bluetooth);
        btnONOFF = (Button) findViewById(R.id.btnONOFF);
        btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
        mBTDevices = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothThread = new HandlerThread("bluetooth thread", 6);
        bluetoothThread.start();
        bluetoothHandler = new Handler(bluetoothThread.getLooper());
        bluetoothHandler.post(new Runnable() {
            BluetoothAdapter mBluetoothAdapter;
            BluetoothDevice mBTDevice;

            //DeviceListAdapter d = new DeviceListAdapter(getApplicationContext());
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {

                try {
                    while (true) {
                        btnONOFF.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                                enableDisableBT();

                            }
                        });
                    }
                } catch (NullPointerException e) {

                }
            }
        });


    }

    public void bluetooth_next(View view) {
        setContentView(R.layout.turn_on_gyroscope);
    }

    public void turn_on_gyroscope(View view) {
        setContentView(R.layout.turn_on_gps);
        sensormanager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscopeThread = new HandlerThread("gyroscope thread", 5);
        gyroscopeThread.start();
        gyroscopeHandler = new Handler(gyroscopeThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        sensormanager.registerListener(sensorEventListener, sensormanager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sensormanager.SENSOR_DELAY_NORMAL, gyroscopeHandler);

    }





    public void turn_on_gps(View view) {
        setContentView(R.layout.covid_tracking);
        gpsThread = new HandlerThread("gps thread", 5);
        gpsThread.start();
        gpsHandler = new Handler(gpsThread.getLooper());
        gpsHandler.post(new Runnable()
        {
            GPStracker g = new GPStracker(getApplicationContext());
            String GpsRecordToSave;
            public void run() {
                while (true) {
                    Date nowforgps = new Date();
                    long GPStimestamp = nowforgps.getTime();
                    if((GPStimestamp-GpsLastWritingTime)>1000){
                        Location l = g.getLocation();
                        if(l != null){
                            lat = l.getLatitude();
                            lon = l.getLongitude();}
                        else{
                            lat = 0;
                            lon = 0;
                        }
                        GpsRecordToSave = sdf.format(GPStimestamp)+" gps "+lon+" "+lat+"\r"+"\n";
                        try{
                            File textfile = new File(MainActivity.this.getFilesDir(),"text");
                            if (!textfile.exists())
                            {
                                textfile.mkdir();
                            }
                            File gyroscopefile = new File(textfile,"gps.txt");
                            FileWriter writer = new FileWriter(gyroscopefile,true);
                            writer.append(GpsRecordToSave);
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            //              e.printStackTrace();
                        }
                        GpsLastWritingTime=GPStimestamp;
                    }
                }
            }
        });

    }

    public void covid_tracking(View view) {

    }

    //define gyroscope event listener//
    private final SensorEventListener sensorEventListener = new SensorEventListener() {


        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

        public void onSensorChanged(SensorEvent event) {
            Date now = new Date();
            long timestamp = now.getTime();
            int NumOfGyroVal;
            float[] values=event.values;
            float x=values[2];
            //double gyroscopeThreshold1=7;
            //double gyroscopeThreshold2=1;
            //double gyroscopeThreshold3=2;
            //double gyroscopeThreshold4=3;
            //System.out.println(timestamp);


            try{
                if((timestamp-GyroscopeLastWritingTime)>1000 && GyroscopeValue.size()>0.5){
                    NumOfGyroVal=GyroscopeValue.size();
                    float total=0;
                    for(int i = 0; i < NumOfGyroVal; i++){
                        total =total+ GyroscopeValue.get(i);}
                    float AverageGyroVal=total/NumOfGyroVal;
                    String textToSave = sdf.format(timestamp)+" gyroscope "+AverageGyroVal+"\r"+"\n";
                    File textfile = new File(MainActivity.this.getFilesDir(),"text");
                    if (!textfile.exists())
                    {
                        textfile.mkdir();
                    }
                    File gyroscopefile = new File(textfile,"gyroscope.txt");
                    FileWriter writer = new FileWriter(gyroscopefile,true);
                    writer.append(textToSave);
                    writer.flush();
                    writer.close();
                    if(lat*lon<0.5){
                        Toast.makeText(getApplicationContext(),
                                "\n  GPS: " +"unavialable"+
                                        "\n  Gyroscope: " +AverageGyroVal
                                        +"\n  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getApplicationContext(),
                                "\n  GPS: " +"LAT: " + lat + "LON: " + lon+
                                        "\n  Gyroscope: " +AverageGyroVal
                                        +"\n  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
                    }

                    //Toast.makeText(getApplicationContext(), "\n LAT: " + lat + "\n LON: " + lon, Toast.LENGTH_LONG).show();
                    //Toast.makeText(getApplicationContext(),"  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
                    GyroscopeLastWritingTime=timestamp;
                    GyroscopeValue.clear();
                }else{
                    GyroscopeValue.add(x);
                }
                //FileOutputStream fileout=openFileOutput("gyroscope", MODE_PRIVATE);
                //OutputStreamWriter outputWriter=new OutputStreamWriter(fileout);
                //outputWriter.append(textToSave);
                //outputWriter.close();

            } catch (IOException e) {
                //              e.printStackTrace();
            }
        }

    };


    public void sqlButton(View view) {
        if (connection != null) {
            Statement statement = null;
            try {
                statement = connection.createStatement();
                //ResultSet resultSet = statement.executeQuery("Select * from TEST_TABLE;");

                //ResultSet resultSet = statement.executeQuery("SELECT C1 FROM TEST_TABLE;");
                Object resultSet = statement.executeUpdate("INSERT INTO TEST_TABLE" + " VALUES ('how')");
//                while (resultSet.next()){
//                    textView.setText(resultSet.getString(1));
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
//        else{
//                textView.setText("Connection is null");
//            }
        }
    }
}

