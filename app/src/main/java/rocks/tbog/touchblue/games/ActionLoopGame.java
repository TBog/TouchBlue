package rocks.tbog.touchblue.games;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import rocks.tbog.touchblue.ui.game.GameViewModel;

public class ActionLoopGame implements Game {
    private static final String TAG = ActionLoopGame.class.getSimpleName();

    private final GameViewModel.GameLoop mGameLoop;
    protected final Handler mHandler;
    protected final GameService mService;
    protected Runnable mCallback = this::next;
    private int mStep;
    private int mWaitTimer = 0;
    private int mTouchJump = -1;

    public ActionLoopGame(@NonNull GameViewModel.GameLoop gameLoop, @NonNull Handler handler, @NonNull GameService service) {
        mGameLoop = gameLoop;
        mHandler = handler;
        mService = service;
        mStep = -1;
    }

    @Override
    public void start() {
        Log.d(TAG, "start");
        mStep = 0;

        mService.showLoading();
        mWaitTimer = 3000;

        postUpdate();
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop");
        stopUpdate();
        mStep = -1;
        mService.showNothing();
    }

    @Override
    public boolean isStarted() {
        return mStep >= 0;
    }

    @Override
    public void touch(@NonNull String address) {
        if (mTouchJump == -1)
            return;
        stopUpdate();
        mStep = 0;
        incStep(mTouchJump);
        next();
    }

    protected void stopUpdate() {
        // stop the update NOW
        mHandler.removeCallbacks(mCallback);
    }

    protected void postUpdate() {
        mHandler.postDelayed(mCallback, mWaitTimer);
    }

    protected GameViewModel.GameAction getCurrentStep() {
        if (mStep < 0 || mStep >= mGameLoop.mLoopList.size())
            return null;
        return mGameLoop.mLoopList.get(mStep);
    }

    protected void incStep(int amount) {
        final var stepCount = mGameLoop.mLoopList.size();
        // loop steps
        mStep = (((mStep + amount) % stepCount) + stepCount) % stepCount;
    }

    protected void next() {
        mWaitTimer = 0;
        var step = getCurrentStep();
        if (step == null)
            return;
        switch (step.mAction) {
            case LED_COLOR:
                mService.showColor(step.mValue);
                break;
            case LED_ANIM:
                mService.showAnim(step.mValue);
                // allow some time for the animation to run
                mWaitTimer = 1000;
                break;
            case WAIT_DURATION:
                mWaitTimer = step.mValue;
                break;
            case TOUCH_JUMP_REL:
                // remember where to jump on touch
                mTouchJump = mStep + step.mValue;
                break;
            case TOUCH_JUMP_TO:
                // remember where to jump on touch
                mTouchJump = step.mValue;
                break;
            case JUMP_REL:
                incStep(step.mValue);
                break;
            case JUMP_TO:
                mStep = 0;
                incStep(step.mValue);
                break;
        }
        if (mWaitTimer <= 0) {
            incStep(1);
            next();
        } else {
            postUpdate();
        }
    }
}
