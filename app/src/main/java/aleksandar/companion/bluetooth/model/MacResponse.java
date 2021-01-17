package aleksandar.companion.bluetooth.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.spongycastle.util.encoders.Base64;

public class MacResponse {

    @SerializedName("response_command")
    private String responseCommand = "REQUEST_MAC_REPLY";

    @SerializedName("hmac_dk")
    private String hmacDk;

    @SerializedName("hmac_sk")
    private String hmacSk;

    public MacResponse(String hmacDk, String hmacSk){
        this.hmacDk = hmacDk;
        this.hmacSk = hmacSk;
    }

    public String serialize(){
        return new Gson().toJson(this);
    }

    public String serializeToBase64(){
        return Base64.toBase64String(new Gson().toJson(this).getBytes());
    }
}
