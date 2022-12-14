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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import rocks.tbog.touchblue.data.ServiceOrCharacteristic;
import rocks.tbog.touchblue.helpers.Serializer;
import rocks.tbog.touchblue.ui.game.GameViewModel;

public class AppViewModel extends AndroidViewModel {

    private final MutableLiveData<List<BleSensorModel>> bleEntryList = new MutableLiveData<>();
    private final MutableLiveData<ServiceOrCharacteristicSet> uuidSet = new MutableLiveData<>();
    private final MutableLiveData<List<GameViewModel.GameLoop>> gameList = new MutableLiveData<>();

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

    public LiveData<List<BleSensorModel>> getBleEntryList() {
        return bleEntryList;
    }

    public LiveData<ServiceOrCharacteristicSet> getUuidSet() {
        return uuidSet;
    }

    public LiveData<List<GameViewModel.GameLoop>> getGameList() {
        return gameList;
    }

    public void setBleEntryList(List<BleSensorModel> entries) {
        bleEntryList.postValue(entries);
    }

    public void setGameList(List<GameViewModel.GameLoop> list) {
        gameList.setValue(list);
    }

    public void updateGameList(ArraySet<String> gameSet) {
        var games = new ArrayList<GameViewModel.GameLoop>(gameSet.size());
        for (Iterator<String> iterator = gameSet.iterator(); iterator.hasNext(); ) {
            String serializedGame = iterator.next();
            var game = Serializer.fromStringOrNull(serializedGame, GameViewModel.GameLoop.class);
            if (game == null) {
                iterator.remove();
                continue;
            }
            games.add(game);
        }
        setGameList(games);
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
    private List<BleSensorModel> getSensorList() {
        var list = bleEntryList.getValue();
        if (list == null)
            list = new ArrayList<>(1);
        else
            list = new ArrayList<>(list);
        return list;
    }

    public BleSensorModel addConnection(ScanResult scanResult) {
        var list = getSensorList();

        // don't add the same address twice
        for (var entry : list) {
            if (entry.equalsResult(scanResult))
                return entry;
        }

        // add address and notify observers
        var bleSensor = new BleSensorModel(scanResult);
        if (list.add(bleSensor))
            bleEntryList.postValue(list);
        return bleSensor;
    }

    public BleSensorModel addConnection(String address, String name) {
        var list = getSensorList();

        // don't add the same address twice
        for (var entry : list) {
            if (entry.getAddress().equals(address))
                return entry;
        }

        // add address and notify observers
        var bleSensor = new BleSensorModel(address, name);
        list.add(bleSensor);
        bleEntryList.setValue(list);
        return bleSensor;
    }

    @Nullable
    public BleSensorModel findSensor(@Nullable String address) {
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
