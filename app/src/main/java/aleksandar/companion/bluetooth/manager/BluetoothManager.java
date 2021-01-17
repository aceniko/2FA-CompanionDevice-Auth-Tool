package aleksandar.companion.bluetooth.manager;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothManager {

    private static final String TAG = "BT-Manager";
    private static Service mService;

    private BluetoothAdapter mBluetoothAdapter;

    private int mState;
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_SEND = 4;

    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;

    private static BluetoothManager mInstance;

    public static BluetoothManager getInstance() {
        return mInstance;
    }

    public static BluetoothManager newInstance(Service _service, BluetoothAdapter _adapter) {
        if(mInstance == null) {
            mInstance = new BluetoothManager(_service, _adapter);
        }
        return getInstance();
    }

    private BluetoothManager(Service _service, BluetoothAdapter _adapter) {
        mService = _service;
        mBluetoothAdapter = _adapter;
        mState = STATE_NONE;
    }

    public synchronized void setState(int state) {
        Log.d(TAG, "setState: " + mState + " -> " + state);
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "[start]");

        // cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread(mBluetoothAdapter);
            mAcceptThread.start();
        }
    }

    public synchronized void stop() {
        Log.d(TAG, "[stop]");

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    public synchronized void connected(BluetoothSocket _socket) {
        Log.d(TAG, "[connected]");

        // cancel the thread currently running a connection
        if(mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if(mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(_socket, mService);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    public BluetoothAdapter getmBluetoothAdapter(){
        return mBluetoothAdapter;
    }
}
