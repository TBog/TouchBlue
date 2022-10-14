package rocks.tbog.touchblue.ui.game;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rocks.tbog.touchblue.AppViewModel;
import rocks.tbog.touchblue.R;
import rocks.tbog.touchblue.databinding.DialogDropDownBinding;
import rocks.tbog.touchblue.databinding.DialogSliderBinding;
import rocks.tbog.touchblue.databinding.FragmentGameCustomizeBinding;
import rocks.tbog.touchblue.helpers.Serializer;

public class GameSetup extends Fragment {

    private FragmentGameCustomizeBinding binding;
    private GameViewModel gameViewModel;
    private AppViewModel appData;

    public GameSetup() {
        //  An empty constructor for Android System to use
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        gameViewModel = new ViewModelProvider(this).get(GameViewModel.class);
        appData = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        binding = FragmentGameCustomizeBinding.inflate(inflater, container, false);
        var gameActionAdapter = new GameActionAdapter(new int[]{R.id.btn_config});
        binding.list.setAdapter(gameActionAdapter);

        gameViewModel.setTitle(inflater.getContext(), R.string.game_setup_title);
        Bundle args = getArguments();
        if (args != null) {
            var gameLoop = args.get("game_loop");
            if (gameLoop instanceof GameViewModel.GameLoop) {
                gameViewModel.setLoop((GameViewModel.GameLoop) gameLoop);
                gameViewModel.setGameName(((GameViewModel.GameLoop) gameLoop).mGameName);
            }
            var gameName = args.get("game_name");
            if (gameName instanceof CharSequence)
                gameViewModel.setGameName((CharSequence) gameName);
        }

        gameViewModel.getTitle().observe(getViewLifecycleOwner(), game -> binding.textTitle.setText(game));
        gameViewModel.getGameName().observe(getViewLifecycleOwner(), game -> binding.textGameName.setText(game));
        gameViewModel.getLoop().observe(getViewLifecycleOwner(), loop -> {
            gameActionAdapter.setGameActionList(loop.mLoopList);
            binding.btnSave.setVisibility(View.VISIBLE);
        });

        gameActionAdapter.setOnButtonClickListener(this::changeActionButton);

        binding.btnSave.setVisibility(View.INVISIBLE);
        binding.btnSave.setOnClickListener(v -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            var gameSet = new ArraySet<>(prefs.getStringSet("game_set", Collections.emptySet()));
            // find current game
            var currentGame = gameViewModel.getLoop().getValue();
            var gameName = currentGame != null ? currentGame.mGameName : gameViewModel.getGameName().getValue();
            for (Iterator<String> iterator = gameSet.iterator(); iterator.hasNext(); ) {
                String serializedGame = iterator.next();
                var game = Serializer.fromStringOrNull(serializedGame, GameViewModel.GameLoop.class);
                if (game == null) {
                    iterator.remove();
                } else if (game.mGameName.compareTo(String.valueOf(gameName)) == 0) {
                    iterator.remove();
                }
            }
            var serializedGame = Serializer.toStringOrNull(gameViewModel.getLoop().getValue());
            if (serializedGame != null && gameSet.add(serializedGame)) {
                appData.updateGameList(gameSet);
                prefs.edit()
                        .putStringSet("game_set", gameSet)
                        .apply();
                Toast.makeText(getContext(), "Game `" + gameName + "` saved!", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void changeActionButton(View buttonView, GameActionAdapter adapter, int groupPosition, int childPosition) {
        if (childPosition == 1) {
            var gameAction = adapter.getGroup(groupPosition);
            changeActionValue(gameAction.mAction, adapter, groupPosition);
        } else if (childPosition == 0)
            changeAction(adapter, groupPosition);
    }

    private void changeAction(GameActionAdapter adapter, int group) {
        var values = GameViewModel.GameAction.Action.values();
        var names = new String[values.length];
        int pos = 0;
        var action = adapter.getGroup(group).mAction;
        for (int i = 0; i < values.length; i++) {
            var value = values[i];
            names[i] = value.toString();
            if (action.equals(value))
                pos = i;
        }
        showDropDownDialog(names, values, pos, value -> {
            adapter.changeAction(group, new GameViewModel.GameAction(value, 0));
            updateGameLoop(adapter.getGameActionList());
        });
    }

    private void updateGameLoop(List<GameViewModel.GameAction> gameActionList) {
        var currentGame = gameViewModel.getLoop().getValue();
        var gameName = currentGame != null ? currentGame.mGameName : gameViewModel.getGameName().getValue();

        var gameLoop = new GameViewModel.GameLoop(gameActionList, String.valueOf(gameName));
        gameViewModel.setLoop(gameLoop);
    }

    interface OnValueSet<T> {
        void onValueSet(T value);
    }

    private <T> void showDropDownDialog(String[] entries, T[] arrValues, int initialSelection, OnValueSet<T> callback) {
        var inflater = LayoutInflater.from(getContext());
        var dlgBind = DialogDropDownBinding.inflate(inflater);
        var adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                entries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dlgBind.dropDown.setAdapter(adapter);
        dlgBind.dropDown.setSelection(initialSelection);
        new AlertDialog.Builder(requireContext())
                .setView(dlgBind.getRoot())
                .setPositiveButton("Set", (dialog, which) -> {
                    var selectedPosition = dlgBind.dropDown.getSelectedItemPosition();
                    var value = selectedPosition < arrValues.length ? arrValues[selectedPosition] : null;
                    callback.onValueSet(value);
                })
                .show();
    }

    private void changeActionValue(GameViewModel.GameAction.Action action, GameActionAdapter adapter, int group) {
        int minValue = 0;
        int maxValue = 1000;
        switch (action) {
            case LED_COLOR:
                maxValue = 0xffffff;
                break;
            case LED_ANIM:
                maxValue = 2;
                break;
            case WAIT_DURATION:
                maxValue = 10000;
                break;
            case TOUCH_JUMP_REL:
            case JUMP_REL:
                minValue = -group;
                maxValue = adapter.getGroupCount() - group;
                break;
            case TOUCH_JUMP_TO:
            case JUMP_TO:
                maxValue = adapter.getGroupCount() - 1;
                break;
        }
        showSliderDialog(adapter.getGroup(group).mValue, minValue, maxValue, value -> {
            adapter.changeActionValue(group, value);
            updateGameLoop(adapter.getGameActionList());
        });
    }

    private void showSliderDialog(int initialValue, int minValue, int maxValue, OnValueSet<Integer> callback) {
        var inflater = LayoutInflater.from(getContext());
        var dlgBind = DialogSliderBinding.inflate(inflater);
        dlgBind.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dlgBind.label.setText(String.valueOf(minValue + progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // noting
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // noting
            }
        });
        dlgBind.slider.setMax(maxValue - minValue);
        dlgBind.slider.setProgress(initialValue - minValue);
        new AlertDialog.Builder(requireContext())
                .setView(dlgBind.getRoot())
                .setPositiveButton("Set", (dialog, which) -> {
                    var value = minValue + dlgBind.slider.getProgress();
                    callback.onValueSet(value);
                })
                .show();
    }
}
