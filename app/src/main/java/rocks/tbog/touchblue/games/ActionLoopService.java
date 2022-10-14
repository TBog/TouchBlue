package rocks.tbog.touchblue.games;

import androidx.annotation.NonNull;

import rocks.tbog.touchblue.BleSensorService;
import rocks.tbog.touchblue.helpers.GattAttributes;

public class ActionLoopService implements GameService{
    @NonNull
    private final BleSensorService mSensorService;
    @NonNull
    private final String mDeviceAddress;

    public ActionLoopService(@NonNull BleSensorService sensorService, @NonNull String address) {
        mSensorService = sensorService;
        mDeviceAddress = address;
    }

    @Override
    public void showLoading() {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 254);  //GSC_LOADING
    }

    @Override
    public void showReady() {
        // not used
    }

    @Override
    public void showError() {
        // not used
    }

    @Override
    public void showValid() {
        // not used
    }

    @Override
    public void showNothing() {
        // not used
    }

    @Override
    public boolean isAddressValid(String address) {
        return mDeviceAddress.equals(address);
    }

    @Override
    public void showColor(int color) {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 100);  //GSC_COLOR
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.LED_COLOR, color & 0xffffff);
    }

    @Override
    public void showAnim(int animIdx) {
        mSensorService.setIntValue(mDeviceAddress, GattAttributes.GAME_STATE, 101 | (animIdx << 8));  //GSC_ANIM
    }
}
