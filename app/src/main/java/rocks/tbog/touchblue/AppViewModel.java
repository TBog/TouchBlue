package rocks.tbog.touchblue;

import android.app.Application;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import rocks.tbog.touchblue.data.ServiceOrCharacteristic;

public class AppViewModel extends AndroidViewModel {

    private final MutableLiveData<List<BleSensor>> bleEntryList = new MutableLiveData<>();
    private final MutableLiveData<ServiceOrCharacteristicSet> uuidSet = new MutableLiveData<>();

    public static class ServiceOrCharacteristicSet {
        private final Set<ServiceOrCharacteristic> allServicesAndCharacteristics = new ArraySet<>();

        public boolean updateServiceAndCharacteristics(BluetoothGattService service) {
            int changes = 0;
            if (allServicesAndCharacteristics.add(ServiceOrCharacteristic.newService(service.getUuid()))) {
                changes += 1;
            }
            for (var characteristic : service.getCharacteristics()) {
                var chInfo = ServiceOrCharacteristic
                        .newCharacteristic(service.getUuid(), characteristic.getUuid());
                if (allServicesAndCharacteristics.add(chInfo)) {
                    changes += 1;
                }
            }
            return changes > 0;
        }

        public boolean updateCharacteristic(UUID serviceUUID, UUID characteristicUUID) {
            int changes = 0;
            if (allServicesAndCharacteristics.add(ServiceOrCharacteristic.newService(serviceUUID))) {
                changes += 1;
            }
            var chInfo = ServiceOrCharacteristic
                    .newCharacteristic(serviceUUID, characteristicUUID);
            if (allServicesAndCharacteristics.add(chInfo)) {
                changes += 1;
            }
            return changes > 0;
        }

        public ArrayList<UUID> getServices() {
            var services = new ArrayList<UUID>();
            for (var item : allServicesAndCharacteristics) {
                if (item.isService())
                    services.add(item.getServiceUUID());
            }
            return services;
        }

        public ArrayList<UUID> getCharacteristics(UUID service) {
            var characteristics = new ArrayList<UUID>();
            for (var item : allServicesAndCharacteristics) {
                if (item.isService() || !item.getServiceUUID().equals(service))
                    continue;
                characteristics.add(item.getCharacteristicUUID());
            }
            return characteristics;
        }

        public ServiceOrCharacteristic findCharacteristic(@NonNull UUID characteristicUUID) {
            for (var item : allServicesAndCharacteristics) {
                if (characteristicUUID.equals(item.getCharacteristicUUID()))
                    return item;
            }
            return null;
        }
    }

    public AppViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<BleSensor>> getBleEntryList() {
        return bleEntryList;
    }

    public LiveData<ServiceOrCharacteristicSet> getUuidSet() {
        return uuidSet;
    }

    public void setBleEntryList(List<BleSensor> entries) {
        bleEntryList.postValue(entries);
    }

    public void updateServiceAndCharacteristics(BluetoothGattService service) {
        boolean changed = false;
        var set = uuidSet.getValue();
        if (set == null) {
            set = new ServiceOrCharacteristicSet();
            changed = true;
        }
        if (set.updateServiceAndCharacteristics(service)) {
            changed = true;
        }
        if (changed)
            uuidSet.setValue(set);
    }

    public void updateCharacteristic(UUID serviceUUID, UUID characteristicUUID) {
        boolean changed = false;
        var set = uuidSet.getValue();
        if (set == null) {
            set = new ServiceOrCharacteristicSet();
            changed = true;
        }
        if (set.updateCharacteristic(serviceUUID, characteristicUUID)) {
            changed = true;
        }
        if (changed)
            uuidSet.setValue(set);
    }

    @NonNull
    private List<BleSensor> getSensorList() {
        var list = bleEntryList.getValue();
        if (list == null)
            list = new ArrayList<>(1);
        else
            list = new ArrayList<>(list);
        return list;
    }

    public BleSensor addConnection(ScanResult scanResult) {
        var list = getSensorList();

        // don't add the same address twice
        for (var entry : list) {
            if (entry.equalsResult(scanResult))
                return entry;
        }

        // add address and notify observers
        var bleSensor = new BleSensor(scanResult);
        if (list.add(bleSensor))
            bleEntryList.postValue(list);
        return bleSensor;
    }

    public BleSensor addConnection(String address, String name) {
        var list = getSensorList();

        // don't add the same address twice
        for (var entry : list) {
            if (entry.getAddress().equals(address))
                return entry;
        }

        // add address and notify observers
        var bleSensor = new BleSensor(address, name);
        list.add(bleSensor);
        bleEntryList.setValue(list);
        return bleSensor;
    }

    @Nullable
    public BleSensor findSensor(@Nullable String address) {
        var list = bleEntryList.getValue();
        if (list == null || address == null)
            return null;
        for (var sensor : list) {
            if (address.equals(sensor.getAddress()))
                return sensor;
        }
        return null;
    }

    @Nullable
    public UUID findServiceForCharacteristic(UUID characteristicUUID) {
        var set = uuidSet.getValue();
        if (set == null)
            return null;
        var characteristic = set.findCharacteristic(characteristicUUID);
        if (characteristic == null)
            return null;
        return characteristic.getServiceUUID();
    }

}
