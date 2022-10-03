package rocks.tbog.touchblue.games;

import androidx.annotation.NonNull;

import rocks.tbog.touchblue.BleSensorService;

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
        mSensorService.setIntValue(mDeviceAddress, BleSensorService.UUID_GAME_STATE, 254);
    }

    @Override
    public void showReady() {
        mSensorService.setIntValue(mDeviceAddress, BleSensorService.UUID_GAME_STATE, 1);
    }

    @Override
    public void showError() {
        mSensorService.setIntValue(mDeviceAddress, BleSensorService.UUID_GAME_STATE, 2);
    }

    @Override
    public void showValid() {
        mSensorService.setIntValue(mDeviceAddress, BleSensorService.UUID_GAME_STATE, 3);
    }

    @Override
    public void showNothing() {
        mSensorService.setIntValue(mDeviceAddress, BleSensorService.UUID_GAME_STATE, 0);
    }

    @Override
    public boolean isAddressValid(String address) {
        return mDeviceAddress.equals(address);
    }
}
