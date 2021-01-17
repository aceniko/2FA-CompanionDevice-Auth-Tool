package aleksandar.companion.bluetooth.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;

import aleksandar.companion.bluetooth.MainActivity;

public class Encryption {
    private static final String TAG = "BT-Enc";
    private static Encryption mInstance;
    private static Context mContext;


    private SharedPreferences sharedpreferences;

    public static Encryption getInstance(Context _context) {
        if (mInstance == null) {
            mContext = _context;
            mInstance = new Encryption();
        }
        return mInstance;
    }

    private Encryption() {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        sharedpreferences = mContext.getSharedPreferences("btProximity", Context.MODE_PRIVATE);
    }

    public void generateDeviceKey(){
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
            //keyGenerator.init(new KeyGenParameterSpec.Builder("authKey", KeyProperties.PURPOSE_SIGN).build());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("deviceKey", Base64.toBase64String(keyGenerator.generateKey().getEncoded()));
        editor.commit();
    }

    public String getDeviceKey(){
        return sharedpreferences.getString("deviceKey", null);
    }

    public String getAuthKey(){
        return sharedpreferences.getString("authKey", null);
    }

    public void generateAuthKey(){
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
            //keyGenerator.init(new KeyGenParameterSpec.Builder("authKey", KeyProperties.PURPOSE_SIGN).build());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("authKey", Base64.toBase64String(keyGenerator.generateKey().getEncoded()));
        editor.commit();

    }

    public String encryptAES128(String plaintext, String key) {
        Log.d(TAG, "Going to encrypt " + plaintext);
        try {
            SharedPreferences sharedpreferences = mContext.getSharedPreferences("btProximity", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();

            byte[] salt = saltGeneration();
            editor.putString("salt", Base64.toBase64String(salt));
            editor.apply();

            /*
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", "SC");
            KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 65536, 256);
            SecretKey tmp = keyFactory.generateSecret(spec);
            */

            // work around for the support of "PBKDF2WithHmacSHA256"
            PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
            generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(key.toCharArray()), salt, 65536);
            KeyParameter keyp = (KeyParameter) generator.generateDerivedParameters(256);

            SecretKey secret = new SecretKeySpec(keyp.getKey(), "AES"); // used to be tmp.getEncoded()

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            AlgorithmParameters parms = cipher.getParameters();
            byte[] iv = parms.getParameterSpec(IvParameterSpec.class).getIV();

            editor.putString("iv", Base64.toBase64String(iv));
            editor.apply();

            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            Log.d(TAG, "encrypted sucessfully");
            return Base64.toBase64String(encrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidParameterSpecException e) {
            Log.d(TAG, "encrypted UNsucessfully!");
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public String decryptAES128(String encrypted, String key) {
        Log.d(TAG, "Going to decrypt " + encrypted);
        String ret = null;
        try {
            SharedPreferences sharedpreferences = mContext.getSharedPreferences("btProximity", Context.MODE_PRIVATE);
            String strIV = sharedpreferences.getString("iv", null);
            String strSalt = sharedpreferences.getString("salt", null);

            if (strIV != null && strSalt != null) {

                byte[] iv = Base64.decode(strIV);
                byte[] salt = Base64.decode(strSalt);

                // work around for the support of "PBKDF2WithHmacSHA256"
                PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
                generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(key.toCharArray()), salt, 65536);
                KeyParameter keyp = (KeyParameter) generator.generateDerivedParameters(256);

                SecretKey secret = new SecretKeySpec(keyp.getKey(), "AES"); // used to be tmp.getEncoded()

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

                byte[] plain = cipher.doFinal(Base64.decode(encrypted));
                ret = new String(plain);
                Log.d(TAG, "decrypted sucessfully -> " + ret);
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            Log.d(TAG, "decrypted UNsucessfully!");
            Log.e(TAG, e.getMessage());
        }
        return ret;
    }

    private byte[] saltGeneration() {
        SecureRandom random = new SecureRandom();
        return random.generateSeed(8);
    }
    public String nonceGenerator(){
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            stringBuilder.append(secureRandom.nextInt(10));
        }
        String randomNumber = stringBuilder.toString();

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("nonce", randomNumber);
        editor.commit();

        return randomNumber;
    }

    public String getNonce(){
        return sharedpreferences.getString("nonce", null);
    }

    public static byte[] createHMAC(byte[] msg, byte[] key) {
        String digest = null;
        try {
            SecretKeySpec keyspec = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyspec);

            byte[] bytes = mac.doFinal(msg);
            return bytes;
            /*
            StringBuilder hash = new StringBuilder();
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }

            digest = hash.toString();
             */
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
