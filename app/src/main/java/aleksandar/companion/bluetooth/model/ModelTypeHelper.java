package aleksandar.companion.bluetooth.model;

import aleksandar.companion.bluetooth.model.constants.Commands;

public class ModelTypeHelper {
    public static Class<?> getTypeFromCommand(String command){
        switch (command){
            case Commands.REQUEST_NOUNCE:
                return RequestNonceModel.class;
            case Commands.REQUEST_MAC:
                return MacRequest.class;
            case Commands.REQUEST_KEYS:
                return RegisterDeviceRequest.class;
            default: return BaseRequest.class;
        }
    }
}
