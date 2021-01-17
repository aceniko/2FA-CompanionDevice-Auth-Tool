package aleksandar.companion.bluetooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.spongycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import aleksandar.companion.bluetooth.R;
import aleksandar.companion.bluetooth.utils.Encryption;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BT-Main";

    private BluetoothAdapter mBluetoothAdapter;
    private SharedPreferences sharedpreferences;
    private Encryption encManager;

    private Switch swService, swBT;
    private ImageView ivBT;


    private final Handler mHandler = new Handler(){

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what){

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is mandatory!", Toast.LENGTH_SHORT).show();
            finish();
        }

        sharedpreferences = getSharedPreferences("btProximity", Context.MODE_PRIVATE);
        encManager = Encryption.getInstance(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        initViews();
        updateViews();
    }

    // Views
    private void initViews() {



        swService = (Switch) findViewById(R.id.switchService);
        swService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService();
                } else {
                    stopService();
                }
                updateViewService();
            }
        });
        swBT = (Switch) findViewById(R.id.swBT);
        swBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableBluetooth();
                } else {
                    // disable Bluetooth
                }
                updateViewBT();
            }
        });

        ivBT = (ImageView) findViewById(R.id.ivBT);
    }

    private void updateViews() {
        updateViewBT(); // check if bluetooth is on
        updateViewService(); // check if Service is running
    }

    private boolean updateViewBT() {
        if (mBluetoothAdapter.isEnabled()) {
            swBT.setChecked(true);
            ivBT.setImageResource(R.drawable.ic_done_black_24dp);
            return true;
        } else {
            swBT.setChecked(false);
            ivBT.setImageResource(R.drawable.ic_error_black_24dp);
            return false;
        }
    }


    private void updateViewService() {
        if (isServiceRunning(MyService.class)) {
            swService.setChecked(true);
        } else {
            swService.setChecked(false);
        }
    }

    // Service
    private void startService() {
        if (!isServiceRunning(MyService.class)) {
            Intent startService = new Intent(this, MyService.class);
            startService(startService);
        }
    }

    private void stopService() {
        Intent stopService = new Intent(this, MyService.class);

        stopService(stopService);
    }

    private boolean isServiceRunning(Class<?> _serviceClass) {
        ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : actManager.getRunningServices(Integer.MAX_VALUE)) {
            if (_serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    // Bluetooth
    private void enableBluetooth() {
        Intent enableIntent = null;
        if (!mBluetoothAdapter.isEnabled()) {
            enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.putExtra(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE, 1);
            enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(enableIntent, 1);
        }
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            if (enableIntent == null) {
                enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            }
            enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(enableIntent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
