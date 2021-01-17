package aleksandar.companion.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import aleksandar.companion.bluetooth.manager.BluetoothManager;
import aleksandar.companion.bluetooth.utils.Encryption;

public class MyService extends Service {

    private static final String TAG = "BT-Service";
    private BluetoothAdapter mBluetoothAdapter;

    private SharedPreferences sharedpreferences;

    private Encryption encManager;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sharedpreferences = getApplicationContext().getSharedPreferences("btProximity", Context.MODE_PRIVATE);

        encManager = Encryption.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BT Service runs!");

        startBluetoothManager();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        BluetoothManager bluetoothManager = BluetoothManager.newInstance(this, mBluetoothAdapter);
        bluetoothManager.stop();
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }

    void startBluetoothManager() {
        BluetoothManager bluetoothManager = BluetoothManager.newInstance(this, mBluetoothAdapter);
        bluetoothManager.start();
        Toast.makeText(getApplicationContext(), "listening for devices", Toast.LENGTH_SHORT).show();
    }

}
