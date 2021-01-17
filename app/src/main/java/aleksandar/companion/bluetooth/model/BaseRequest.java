package aleksandar.companion.bluetooth.model;

import com.google.gson.annotations.SerializedName;

public class BaseRequest {
    @SerializedName("request_command")
    protected String requestCommand;

    public String getRequestCommand(){ return requestCommand; }
}
