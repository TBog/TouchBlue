package rocks.tbog.touchblue.ui.game;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GameViewModel extends ViewModel {

    private final MutableLiveData<String> mTitle = new MutableLiveData<>("");
    private final MutableLiveData<CharSequence> mGameName = new MutableLiveData<>("");
    private final MutableLiveData<GameLoop> mLoop = new MutableLiveData<>(GameLoop.DEFAULT_GAME);

    public static class GameAction implements Serializable {
        private static final long serialVersionUID = 1L;

        public Action mAction;
        public int mValue;

        public enum Action {
            LED_COLOR,
            LED_ANIM,
            WAIT_DURATION,
            TOUCH_JUMP_REL,
            TOUCH_JUMP_TO,
            JUMP_REL,
            JUMP_TO
        }

        public GameAction() {
            this(Action.WAIT_DURATION, 1000); // 1000 ms = 1 second
        }

        public GameAction(@NonNull Action action, int value) {
            mAction = action;
            mValue = value;
        }
    }

    public static class GameLoop implements Serializable {
        private static final long serialVersionUID = 1L;

        public final List<GameAction> mLoopList;
        public final String mGameName;

        public static final GameLoop DEFAULT_GAME = new GameLoop(Arrays.asList(
                new GameAction(GameViewModel.GameAction.Action.TOUCH_JUMP_TO, 6),
                new GameAction(GameViewModel.GameAction.Action.LED_COLOR, Color.BLACK),
                new GameAction(GameViewModel.GameAction.Action.WAIT_DURATION, 100),
                new GameAction(GameViewModel.GameAction.Action.LED_COLOR, Color.BLUE),
                new GameAction(GameViewModel.GameAction.Action.TOUCH_JUMP_REL, 5),
                new GameAction(GameViewModel.GameAction.Action.WAIT_DURATION, 1000),
                new GameAction(GameViewModel.GameAction.Action.LED_COLOR, Color.RED),
                new GameAction(GameViewModel.GameAction.Action.WAIT_DURATION, 500),
                new GameAction(GameViewModel.GameAction.Action.JUMP_TO, 0),
                new GameAction(GameViewModel.GameAction.Action.LED_COLOR, Color.GREEN),
                new GameAction(GameViewModel.GameAction.Action.WAIT_DURATION, 500)
        ), "Default game");

        public GameLoop(String gameName) {
            this(Collections.emptyList(), gameName);
        }

        public GameLoop(Collection<GameAction> list, String gameName) {
            mLoopList = new ArrayList<>(list);
            mGameName = gameName;
        }

        public void addAction(@NonNull GameAction.Action action, int value) {
            mLoopList.add(new GameAction(action, value));
        }
    }

    public LiveData<String> getTitle() {
        return mTitle;
    }

    public LiveData<CharSequence> getGameName() {
        return mGameName;
    }

    public LiveData<GameLoop> getLoop() {
        return mLoop;
    }

    public void setTitle(@NonNull Context context, @StringRes int title) {
        mTitle.setValue(context.getString(title));
    }

    public void setGameName(@NonNull CharSequence name) {
        mGameName.setValue(name);
    }

    public void setLoop(GameLoop loop) {
        mLoop.setValue(loop);
    }
}
