package rocks.tbog.touchblue.ui.game;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import rocks.tbog.touchblue.BleSensorService;
import rocks.tbog.touchblue.R;
import rocks.tbog.touchblue.databinding.FragmentGameCustomizeBinding;

public class GameSetup extends Fragment {

    private FragmentGameCustomizeBinding binding;

    public GameSetup() {
        //  An empty constructor for Android System to use
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        var gameViewModel = new ViewModelProvider(this).get(GameViewModel.class);

        binding = FragmentGameCustomizeBinding.inflate(inflater, container, false);
        var gameActionAdapter = new GameActionAdapter(new int[]{R.id.btn_config});
        binding.list.setAdapter(gameActionAdapter);

        gameViewModel.setTitle(inflater.getContext(), R.string.game_setup_title);
        Bundle args = getArguments();
        if (args != null) {
            var gameName = args.get("game_name");
            if (gameName instanceof CharSequence)
                gameViewModel.setGameName((CharSequence) gameName);
        }

        gameViewModel.getTitle().observe(getViewLifecycleOwner(), game -> binding.textTitle.setText(game));
        gameViewModel.getGameName().observe(getViewLifecycleOwner(), game -> binding.textGameName.setText(game));
        gameViewModel.getList().observe(getViewLifecycleOwner(), gameActionAdapter::setGameActionList);

        gameActionAdapter.setOnButtonClickListener((buttonView, adapter, groupPosition, childPosition) -> {
            Toast.makeText(getContext(), "WIP", Toast.LENGTH_SHORT).show();
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void sendIntentToService(@NonNull final Intent intent) {
        var ctx = requireActivity();
        var i = new Intent(intent);
        i.setComponent(new ComponentName(ctx, BleSensorService.class));
        ContextCompat.startForegroundService(ctx, i);
    }


}
