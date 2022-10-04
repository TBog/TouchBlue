package rocks.tbog.touchblue.games;

import androidx.annotation.NonNull;

import rocks.tbog.touchblue.BleSensorService;
import rocks.tbog.touchblue.helpers.GattAttributes;

public class TouchGameService implements GameService {
    @NonNull
    private final BleSensorService mSensorService;
    @NonNull
    private final String mDeviceAddress;

    public TouchGameService(@NonNull BleSensorService sensorService, @NonNull String address) {
        mSensorService = sensorService;
        mDeviceAddress = address;
    }

    @Override
    public void showLoading() {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 254);  //GSC_LOADING
    }

    @Override
    public void showReady() {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 1); //GSC_TOUCH_READY
    }

    @Override
    public void showError() {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 2); //GSC_TOUCH_ERROR
    }

    @Override
    public void showValid() {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 3); //GSC_TOUCH_VALID
    }

    @Override
    public void showNothing() {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 0); //GSC_TOUCH_NOTHING
    }

    @Override
    public boolean isAddressValid(String address) {
        return mDeviceAddress.equals(address);
    }
}
