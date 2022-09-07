package rocks.tbog.touchblue.helpers;

import android.Manifest;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.touchblue.BleEntry;

public class BleScan {
    private static final String TAG = BleScan.class.getSimpleName();
    private final BluetoothLeScanner mScanner;
    private boolean mIsScanning = false;
    private final MutableLiveData<List<BleEntry>> mEntryLiveData = new MutableLiveData<>(Collections.emptyList());
    private final ArrayList<BleEntry> mList = new ArrayList<>(0);
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            var device = result.getDevice();
            var name = result.getScanRecord().getDeviceName();
            Log.i(TAG, "BLE device `" + name + "` address " + device.getAddress());
            addScanResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "Scan failed: " + errorCode);
        }
    };

    public BleScan() {
        mScanner = null;
    }

    public BleScan(BluetoothLeScanner scanner) {
        mScanner = scanner;
    }

    public boolean startScan(@NonNull Context context, ScanSettings scanSettings) {
        if (mScanner == null)
            return false;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            mScanner.startScan(null, scanSettings, mScanCallback);
            mIsScanning = true;
            return true;
        }
        return false;
    }

    private void addScanResult(@NonNull ScanResult result) {
        mIsScanning = true;

        boolean shouldAddNewEntry = true;
        // check if the same address already found
        for (var entry : mList) {
            if (entry.scanResult.getDevice().getAddress().equals(result.getDevice().getAddress())) {
                entry.scanResult = result;
                shouldAddNewEntry = false;
                break;
            }
        }

        if (shouldAddNewEntry) {
            BleEntry bleEntry = new BleEntry();
            bleEntry.scanResult = result;
            mList.add(bleEntry);
        }

        // notify live data of the changed list
        mEntryLiveData.postValue(mList);
    }

    public void stopScan(@NonNull Context context) {
        mIsScanning = false;
        if (mScanner == null)
            return;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
            mScanner.stopScan(mScanCallback);
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public LiveData<List<BleEntry>> getScanList() {
        return mEntryLiveData;
    }
}
