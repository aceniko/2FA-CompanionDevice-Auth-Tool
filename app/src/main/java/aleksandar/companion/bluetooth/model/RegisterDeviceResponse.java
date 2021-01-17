package aleksandar.companion.bluetooth.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.spongycastle.util.encoders.Base64;

public class RegisterDeviceResponse {
    @SerializedName("response_command")
    private String responseCommand = "REQUEST_KEYS_REPLY";

    @SerializedName("device_key")
    private String deviceKeyBase64;

    @SerializedName("auth_key")
    private String authKeyBase64;

    @SerializedName("device_name")
    private String deviceName;

    @SerializedName("device_model")
    private String deviceModel;

    public RegisterDeviceResponse(String deviceKeyBase64, String authKeyBase64, String deviceName, String deviceModel){
        this.deviceKeyBase64 = deviceKeyBase64;
        this.authKeyBase64 = authKeyBase64;
        this.deviceModel = deviceModel;
        this.deviceName = deviceName;
    }



    public String serialize(){
        return new Gson().toJson(this);
    }

    public String serializeToBase64(){
        return Base64.toBase64String(new Gson().toJson(this).getBytes());
    }
}
