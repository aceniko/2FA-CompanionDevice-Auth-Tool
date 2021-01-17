package aleksandar.companion.bluetooth.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.spongycastle.util.encoders.Base64;

public class NonceResponse {
    @SerializedName("response_command")
    private String responseCommand = "REQUEST_NONCE_REPLY";

    @SerializedName("nonce")
    private String nonce;

    public NonceResponse(String nonce){
        this.nonce = nonce;
    }

    public String serialize(){
        return new Gson().toJson(this);
    }

    public String serializeToBase64(){
        return Base64.toBase64String(new Gson().toJson(this).getBytes());
    }
}
