package aleksandar.companion.bluetooth.model;

import com.google.gson.annotations.SerializedName;

public class MacRequest extends BaseRequest {
    @SerializedName("srv_nonce")
    private String srvNonce;
    @SerializedName("sess_nonce")
    private String sessNonce;
    @SerializedName("dev_nonce")
    private String devNonce;

    public String getSrvNonce(){return srvNonce;}
    public String getSessNonce(){return sessNonce;}
    public String getDevNonce(){return devNonce;}

}
