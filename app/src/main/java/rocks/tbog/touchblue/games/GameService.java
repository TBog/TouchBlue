package rocks.tbog.touchblue.games;

public interface GameService {

    void showLoading();

    void showReady();

    void showError();

    void showValid();

    void showNothing();

    boolean isAddressValid(String address);

}
