package rocks.tbog.touchblue.games;

import androidx.annotation.NonNull;

public interface Game {

    void start();

    void stop();

    boolean isStarted();

    void touch(@NonNull String address);
}
