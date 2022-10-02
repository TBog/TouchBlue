package rocks.tbog.touchblue.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeviceSettingsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public DeviceSettingsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Device settings fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}