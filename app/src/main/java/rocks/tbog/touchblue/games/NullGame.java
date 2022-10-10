package rocks.tbog.touchblue.games;

import androidx.annotation.NonNull;

public class NullGame implements Game {
    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void stop() {
        // do nothing
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public void touch(@NonNull String address) {
        // do nothing
    }
}
