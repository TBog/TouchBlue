package rocks.tbog.touchblue.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

import rocks.tbog.touchblue.AppViewModel;

public class ServiceOrCharacteristic {
    @NonNull
    private final UUID serviceUUID;
    @Nullable
    private final UUID characteristicUUID;

    public static ServiceOrCharacteristic newService(@NonNull UUID service) {
        return new ServiceOrCharacteristic(service, null);
    }

    public static ServiceOrCharacteristic newCharacteristic(@NonNull UUID service, @NonNull UUID characteristic) {
        return new ServiceOrCharacteristic(service, characteristic);
    }

    private ServiceOrCharacteristic(@NonNull UUID service, @Nullable UUID characteristic) {
        this.serviceUUID = service;
        this.characteristicUUID = characteristic;
    }

    public boolean isService() {
        return characteristicUUID == null;
    }

    @NonNull
    public UUID getServiceUUID() {
        return serviceUUID;
    }

    @Nullable
    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceOrCharacteristic that = (ServiceOrCharacteristic) o;
        return serviceUUID.equals(that.serviceUUID) && Objects.equals(characteristicUUID, that.characteristicUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceUUID, characteristicUUID);
    }
}
