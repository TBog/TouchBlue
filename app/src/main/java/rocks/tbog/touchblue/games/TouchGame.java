package rocks.tbog.touchblue.games;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Random;

public class TouchGame extends Game {
    private static final String TAG = TouchGame.class.getSimpleName();

    enum State {
        NOT_INITIALIZED,
        STARTED,
        WAIT_BEFORE_TOUCH,
        WAIT_FOR_TOUCH,
        ENDED
    }

    protected State mState = State.NOT_INITIALIZED;
    protected final Handler mHandler;
    protected final GameService mService;
    protected Runnable mCallback = this::next;
    protected final Random randomNumberGenerator = new Random();
    protected int mDelay = 0;
    protected int mMinWaitBefore = 1000;
    protected int mMaxWaitBefore = 3000;
    protected int mWaitForTouch = 1000;
    protected int mErrorTime = 500;

    public TouchGame(@NonNull Handler handler, @NonNull GameService service) {
        mHandler = handler;
        mService = service;
    }

    @Override
    public void start() {
        Log.d(TAG, "start");
        randomNumberGenerator.setSeed(hashCode());
        mState = State.STARTED;
        mDelay = mMaxWaitBefore;
        mService.showLoading();
        postUpdate();
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop");
        mState = State.ENDED;
        stopUpdate();
        mService.showNothing();
    }

    @Override
    public boolean isStarted() {
        return mState.ordinal() >= State.STARTED.ordinal() &&
                mState.ordinal() < State.ENDED.ordinal();
    }

    protected void stopUpdate() {
        if (mHandler == null)
            return;
        // stop the update NOW
        mHandler.removeCallbacks(mCallback);
    }

    protected void postUpdate() {
        if (mHandler == null)
            return;
        mHandler.postDelayed(mCallback, mDelay);
    }

    public void touch(@NonNull String address) {
        if (!mService.isAddressValid(address)) {
            Log.d(TAG, "touch from wrong device");
            return;
        }
        Log.d(TAG, "touch when state=" + mState);
        switch (mState) {
            case WAIT_BEFORE_TOUCH:
                stopUpdate();
                mState = State.STARTED;
                postError();
                break;
            case WAIT_FOR_TOUCH:
                stopUpdate();
                postSuccess();
                mState = State.STARTED;
                break;
            default:
                // if touch called while we display error/success, ignore it!
                return;
        }
        postUpdate();
    }

    protected void next() {
        Log.d(TAG, "next when state=" + mState);
        switch (mState) {
            case STARTED:
                mState = State.WAIT_BEFORE_TOUCH;
                prepareNext();
                break;
            case WAIT_BEFORE_TOUCH:
                mState = State.WAIT_FOR_TOUCH;
                notifyPlayer();
                break;
            case WAIT_FOR_TOUCH:
                mState = State.STARTED;
                postError();
                break;
            default:
                return;
        }
        postUpdate();
    }

    protected void notifyPlayer() {
        mService.showReady();
        mDelay = mWaitForTouch;
    }

    protected void postError() {
        mService.showError();
        mDelay = mErrorTime;
    }

    protected void postSuccess() {
        mService.showValid();
    }

    protected void prepareNext() {
        mService.showNothing();

        int delta = mMaxWaitBefore - mMinWaitBefore;
        mDelay = randomNumberGenerator.nextInt(delta);
        mDelay += mMinWaitBefore;
        Log.d(TAG, "delay=" + mDelay);
    }
}
