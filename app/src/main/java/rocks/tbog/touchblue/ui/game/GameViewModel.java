package rocks.tbog.touchblue.ui.game;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

public class GameViewModel extends ViewModel {

    private final MutableLiveData<String> mTitle = new MutableLiveData<>("");
    private final MutableLiveData<CharSequence> mGameName = new MutableLiveData<>("");
    private final MutableLiveData<List<GameAction>> mLoopList = new MutableLiveData<>(Collections.emptyList());

    public static class GameAction {
        public Action mAction = Action.WAIT_DURATION;
        public int mValue = 1000; // 1000 ms = 1 second

        public enum Action {
            LED_COLOR,
            WAIT_DURATION
        }
    }

    public LiveData<String> getTitle() {
        return mTitle;
    }

    public LiveData<CharSequence> getGameName() {
        return mGameName;
    }

    public void setTitle(@NonNull Context context, @StringRes int title) {
        mTitle.setValue(context.getString(title));
    }

    public void setGameName(@NonNull CharSequence name) {
        mGameName.setValue(name);
    }

    public LiveData<List<GameAction>> getList() {
        return mLoopList;
    }
}
