package com.example.copiedbtcode;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.logging.Logger;


public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private static final String TAG ="BluetoothName" ;
    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int  mViewResourceId;


    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices){
        super(context, tvResourceId,devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }



    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDevice device = mDevices.get(position);

        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceName2 = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceName3 = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAdress = (TextView) convertView.findViewById(R.id.tvDeviceAddress);


            if (deviceName != null) {
                deviceName.setText(device.getName());
                String data = device.getName();
                String DeviceName = deviceName.getText().toString();
                String DeviceName2 = deviceName.getText().toString();
                String DeviceName3 = deviceName.getText().toString();

                File path = getFilesDir(getContext());
                File file = new File(path, "UsersName.txt");
                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Create file failed. ");
                }
                try {
                    assert stream != null;
                    try {
                        stream.write(DeviceName.getBytes());
                        stream.write(DeviceName2.getBytes());
                        stream.write(DeviceName3.getBytes());

                    } catch (IOException e) {
                        Log.e(TAG, "Write data failed. ");
                    }
                } finally {
                    try {
                        assert stream != null;
                        stream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Close stream failed. ");
                    }
                }


                //File file = new File(path, "my-file-name.txt");

            }
            if (deviceAdress != null) {
                deviceAdress.setText(device.getAddress());
            }
        }

        return convertView;
    }

    private File getFilesDir(Context context) {
        File path=context.getFilesDir();
        return path;
    }


}