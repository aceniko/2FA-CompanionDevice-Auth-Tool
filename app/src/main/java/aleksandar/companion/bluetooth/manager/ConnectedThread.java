package aleksandar.companion.bluetooth.manager;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;
import org.spongycastle.jcajce.provider.symmetric.ARC4;
import org.spongycastle.util.encoders.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import aleksandar.companion.bluetooth.model.MacRequest;
import aleksandar.companion.bluetooth.model.MacResponse;
import aleksandar.companion.bluetooth.model.ModelTypeHelper;
import aleksandar.companion.bluetooth.model.NonceResponse;
import aleksandar.companion.bluetooth.model.RegisterDeviceRequest;
import aleksandar.companion.bluetooth.model.RegisterDeviceResponse;
import aleksandar.companion.bluetooth.model.RequestNonceModel;
import aleksandar.companion.bluetooth.model.constants.Commands;
import aleksandar.companion.bluetooth.utils.Encryption;

public class ConnectedThread extends Thread {
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }
    private static final String TAG = "BT-ConnT";

    private Context mContext;
    private static Service mService;

    private final BluetoothSocket bluetoothSocket;

    private final InputStream mInputStream;
    private final OutputStream mOutputStream;

    private Encryption encManager;
    private byte[] tempValue;
    private boolean expect_otp = false;
    private boolean awaits_proof = false;
    private Handler handler;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private BluetoothManager bluetoothManager;
    private boolean flowCompleted = false;
    public ConnectedThread(BluetoothSocket _socket, Service _service) {
        mService = _service;
        bluetoothSocket = _socket;

        encManager = Encryption.getInstance(mService);

        InputStream input = null;
        OutputStream output = null;

        try {
            input = _socket.getInputStream();
            output = _socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "socket streams not created", e);
        }

        mInputStream = input;
        mOutputStream = output;
    }

    @Override
    public void run() {
        Log.d(TAG, "BEGIN CONNECTEDthread");
        mmBuffer = new byte[1024];
        int availableBytes = 0;
        int numBytes; // bytes returned from read()
        bluetoothManager = BluetoothManager.getInstance();
        BufferedReader reader;
        OutputStreamWriter writer;

        // Keep listening to the inputStream while connected
        while (!flowCompleted) {
            try {
                availableBytes = mInputStream.available();
                if(availableBytes>0) {

                    byte[] buffer = new byte[availableBytes];
                    // Read from the InputStream.
                    numBytes = mInputStream.read(buffer);
                    String readMessage = new String(buffer, 0, numBytes);
                    readMessage = readMessage.replace("\n", "").replace("\r", "");

                    //writer = new OutputStreamWriter(mOutputStream);

                    String message = reply(readMessage.getBytes());

                    mOutputStream.write(message.getBytes());
                    mOutputStream.flush();
                    Log.d(TAG, "received: " + readMessage);



                }



                /*
                if (receivedString.length() != 0) {
                    writer = new OutputStreamWriter(mOutputStream);

                    byte[] decoded = Base64.decode(receivedString);
                    String payload = clientReply(decoded);
                    Log.d(TAG, "payload: " + payload);

                    // write send payload to client
                    writer.write(payload + "\r\n");
                    writer.flush();

                }*/
            } catch (IOException e) {
                Log.e(TAG, "DISCONNECTED!");
                // start over
                bluetoothManager.start();
                break;
            }
        }

        bluetoothManager.start();
    }

    public void cancel() {
        Log.d(TAG, "CANCEL CONNECTEDthread");
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connected socket failed", e);
        }
    }

    private synchronized String reply(byte[] input){
        byte[] decoded = Base64.decode(input);
        String str = new String(decoded, StandardCharsets.UTF_8);

        try {
            JSONObject json = new JSONObject(str);
            String command = json.getString("request_command");

            Gson gson = new GsonBuilder().create();
            Class<?> modelType = ModelTypeHelper.getTypeFromCommand(command);

            Object request = gson.fromJson(str, modelType);

            switch (command) {
                case Commands.REQUEST_KEYS: {
                    if(request instanceof RegisterDeviceRequest){
                        encManager.generateDeviceKey();
                        encManager.generateAuthKey();

                        RegisterDeviceResponse response = new RegisterDeviceResponse(encManager.getDeviceKey(), encManager.getAuthKey(), getLocalBluetoothName(), getDeviceName());

                        return response.serializeToBase64() + "\n";
                    }

                }
                case Commands.REQUEST_NOUNCE:{
                    if (request instanceof RequestNonceModel){
                        Log.d(TAG, "REQUEST_NOUNCE");
                        String nonce = Base64.toBase64String(encManager.nonceGenerator().getBytes());
                        return new NonceResponse(nonce).serializeToBase64()+ "\n";
                    }
                }

                case Commands.REQUEST_MAC:{
                    if(request instanceof MacRequest){
                        Log.d(TAG, "MacRequest");

                        byte[] nonceByteArray = encManager.getNonce().getBytes();
                        byte[] devNonceByteArray = Base64.decode(((MacRequest) request).getDevNonce());
                        byte[] sessNonceByteArray = Base64.decode(((MacRequest) request).getSessNonce());

                        byte[] allByteArray = new byte[nonceByteArray.length + devNonceByteArray.length + sessNonceByteArray.length];

                        ByteBuffer buff = ByteBuffer.wrap(allByteArray);
                        buff.put(nonceByteArray);
                        buff.put(devNonceByteArray);
                        buff.put(sessNonceByteArray);

                        byte[] comb = buff.array();

                        String serviceHmac = Base64.toBase64String(encManager.createHMAC(comb, Base64.decode(encManager.getAuthKey())));
                        Log.d(TAG, "Service MAC: "+serviceHmac);
                        Log.d(TAG, "SrvNonceMAC: " + ((MacRequest) request).getSrvNonce());


                        if(((MacRequest) request).getSrvNonce().equals(serviceHmac)){
                            Log.d(TAG, "Ednakvi MAC-ovi");
                            byte[] nonceMac = encManager.createHMAC(devNonceByteArray, Base64.decode(encManager.getDeviceKey()));
                            Log.d(TAG, "nonceMac: "+Base64.toBase64String(nonceMac));

                            byte[] serviceHmacByteArray = Base64.decode(serviceHmac);
                            //byte[] sessionNonceByteArray = ((MacRequest) request).getSessNonce().getBytes();

                            byte[] combined = new byte[nonceMac.length+sessNonceByteArray.length];

                            for(int i = 0; i<nonceMac.length;i++){
                                combined[i] = nonceMac[i];
                            }
                            for(int i = 0; i< sessNonceByteArray.length;i++){
                                combined[nonceMac.length+i] = sessNonceByteArray[i];
                            }

                            String sessionHmac = Base64.toBase64String(encManager.createHMAC(combined, Base64.decode(encManager.getAuthKey())));

                            MacResponse macResponse = new MacResponse(Base64.toBase64String(nonceMac), sessionHmac);

                            flowCompleted = true;

                            return macResponse.serializeToBase64()+ "\n";

                        }
                    }
                }


                default:
                    return "";
            }
        }catch (Exception e){
            return null;
        }
    }
    public String getLocalBluetoothName(){
        if(bluetoothManager == null){
            bluetoothManager = BluetoothManager.getInstance();
        }
        String name = bluetoothManager.getmBluetoothAdapter().getName();
        if(name == null){
            System.out.println("Name is null!");
            name = bluetoothManager.getmBluetoothAdapter().getAddress();
        }
        return name;
    }

    /** Returns the consumer friendly device name */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }


}
