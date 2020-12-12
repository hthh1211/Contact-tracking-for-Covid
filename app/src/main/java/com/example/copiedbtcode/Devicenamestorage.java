package com.example.copiedbtcode;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class Devicenamestorage extends AppCompatActivity {
    private static final String FILE_NAME = "devicename.txt";

    EditText mEditText;
    
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_adapter_view);
        mEditText = findViewById(R.id.lvNewDevices);

    }

}
