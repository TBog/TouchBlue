package rocks.tbog.touchblue.ui.services;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ServicesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ServicesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Services");
    }

    public LiveData<String> getText() {
        return mText;
    }
}