package rocks.tbog.touchblue;

import android.app.Application;
import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class AppViewModel extends AndroidViewModel {

    public AppViewModel(@NonNull Application application) {
        super(application);
    }

    private final MutableLiveData<List<BleEntry>> bleEntryList = new MutableLiveData<>();

    public LiveData<List<BleEntry>> getBleEntryList() {
        return bleEntryList;
    }

    public void setBleEntryList(List<BleEntry> entries) {
        bleEntryList.postValue(entries);
    }

    @NonNull
    private List<BleEntry> getArrayList() {
        var list = bleEntryList.getValue();
        if (list == null)
            list = new ArrayList<>(1);
        else
            list = new ArrayList<>(list);
        return list;
    }

    public void addConnection(ScanResult scanResult) {
        var list = getArrayList();

        // don't add the same address twice
        for (var entry : list) {
            if (entry.equalsResult(scanResult))
                return;
        }

        // add address and notify observers
        list.add(new BleEntry(scanResult));
        bleEntryList.postValue(list);
    }

    public BleEntry addConnection(String address) {
        var list = getArrayList();

        // don't add the same address twice
        for (var entry : list) {
            if (entry.getAddress().equals(address))
                return entry;
        }

        // add address and notify observers
        var entry = new BleEntry(address);
        list.add(entry);
        bleEntryList.postValue(list);
        return entry;
    }
}
