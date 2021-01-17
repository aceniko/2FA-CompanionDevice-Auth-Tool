package aleksandar.companion.bluetooth.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class AcceptThread extends Thread {

    private static final String TAG = "BT-AcceptThread";

    private final UUID MY_UUID = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66");
    private BluetoothServerSocket mBluetoothServerSocket;

    public AcceptThread(BluetoothAdapter _adapter) {
        BluetoothServerSocket btServerSocket = null;

        try {
            //btServerSocket = _adapter.listenUsingInsecureRfcommWithServiceRecord("btProximity", MY_UUID);
            btServerSocket = _adapter.listenUsingInsecureRfcommWithServiceRecord("btProximity", MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "AcceptThread: listen() failed");
        }
        mBluetoothServerSocket = btServerSocket;
    }

    @Override
    public void run() {
        BluetoothSocket bluetoothSocket;
        BluetoothManager bluetoothManager = BluetoothManager.getInstance();

        // Listen to the server socket if we are not connected
        while (bluetoothManager.getState() != BluetoothManager.STATE_CONNECTED) {
            try {
                bluetoothSocket = mBluetoothServerSocket.accept(); // method call blocks
            } catch (IOException e) {
                Log.e(TAG, "accept() failed");
                break;
            }

            // IF a connection was accepted
            if (bluetoothSocket != null) {
                synchronized (bluetoothManager) {
                    switch (bluetoothManager.getState()) {
                        case BluetoothManager.STATE_LISTEN:
                        case BluetoothManager.STATE_CONNECTING:
                            // Situation normal. Start the connected Thread.
                            bluetoothManager.connected(bluetoothSocket);
                            break;
                        case BluetoothManager.STATE_NONE:
                        case BluetoothManager.STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket
                            try {
                                bluetoothSocket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Cloud not close unwanted socket", e);
                            }
                            break;
                    }
                }
            }
        }
    }

    public void cancel() {
        try {
            mBluetoothServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of Server Socket failed", e);
        }
    }
}
