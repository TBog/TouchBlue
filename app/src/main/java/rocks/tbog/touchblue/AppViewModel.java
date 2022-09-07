package rocks.tbog.touchblue;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
}
