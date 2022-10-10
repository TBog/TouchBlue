package rocks.tbog.touchblue.games;

import rocks.tbog.touchblue.BleSensorService;

/**
 * Communication layer between a game and the {@link BleSensorService}
 */
public interface GameService {

    void showLoading();

    void showReady();

    void showError();

    void showValid();

    void showNothing();

    boolean isAddressValid(String address);

}
