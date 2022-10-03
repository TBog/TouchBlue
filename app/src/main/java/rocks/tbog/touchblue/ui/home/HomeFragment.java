package rocks.tbog.touchblue.ui.home;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.UUID;

import rocks.tbog.touchblue.AppViewModel;
import rocks.tbog.touchblue.BleSensor;
import rocks.tbog.touchblue.BleSensorService;
import rocks.tbog.touchblue.R;
import rocks.tbog.touchblue.databinding.DialogSliderBinding;
import rocks.tbog.touchblue.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AppViewModel appData;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        final var adapter = new BleSensorAdapter();
        binding.list.setAdapter(adapter);
        registerForContextMenu(binding.list);

        appData = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        appData.getBleEntryList().observe(getViewLifecycleOwner(), adapter::setList);

        return root;
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        requireActivity().getMenuInflater().inflate(R.menu.home_context, menu);
        var info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        var entry = ((BleSensorAdapter) binding.list.getAdapter()).getItem(info.position);
        menu.setHeaderTitle(entry.getAddress());
        menu.findItem(R.id.action_connect).setOnMenuItemClickListener(item -> {
            var i = new Intent(BleSensorService.ACTION_CONNECT_TO);
            i.putExtra(BleSensorService.EXTRA_ADDRESS, entry.getAddress());
            sendIntentToService(i);
            return true;
        });
        menu.findItem(R.id.action_toggle).setOnMenuItemClickListener(item -> {
            var i = new Intent(BleSensorService.ACTION_TOGGLE_LED);
            i.putExtra(BleSensorService.EXTRA_ADDRESS, entry.getAddress());
            sendIntentToService(i);
            return true;
        });
        menu.findItem(R.id.action_set_brightness)
                .setOnMenuItemClickListener(item -> changeLedSetting(entry, BleSensorService.UUID_LED_BRIGHTNESS));
        menu.findItem(R.id.action_set_saturation)
                .setOnMenuItemClickListener(item -> changeLedSetting(entry, BleSensorService.UUID_LED_SATURATION));
        menu.findItem(R.id.action_game_start).setOnMenuItemClickListener(item -> {
            var i = new Intent(BleSensorService.ACTION_START_GAME);
            i.putExtra(BleSensorService.EXTRA_ADDRESS, entry.getAddress());
            sendIntentToService(i);
            return true;
        });
        menu.findItem(R.id.action_game_stop).setOnMenuItemClickListener(item -> {
            var i = new Intent(BleSensorService.ACTION_STOP_GAME);
            i.putExtra(BleSensorService.EXTRA_ADDRESS, entry.getAddress());
            sendIntentToService(i);
            return true;
        });
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

    private boolean changeLedSetting(BleSensor sensor, UUID characteristic) {
        var o = sensor.getDataValue(characteristic);
        if (o instanceof Integer) {
            var value = (Integer) o;
            showSliderDialog(sensor.getAddress(), characteristic, value);
        } else {
            var i = new Intent(BleSensorService.ACTION_REQUEST_DATA);
            i.putExtra(BleSensorService.EXTRA_ADDRESS, sensor.getAddress());
            i.putExtra(BleSensorService.EXTRA_DATA_UUID, characteristic);
            final Observer<Object> observer = new Observer<>() {
                @Override
                public void onChanged(Object data) {
                    if (data instanceof Integer) {
                        sensor.getData(characteristic).removeObserver(this);

                        var value = (Integer) data;
                        HomeFragment.this.showSliderDialog(sensor.getAddress(), characteristic, value);
                    }
                }
            };
            sensor.getData(characteristic).observe(getViewLifecycleOwner(), observer);
            sendIntentToService(i);
        }
        return true;
    }

    private void showSliderDialog(String address, UUID characteristicUUID, int currentValue) {
        var inflater = LayoutInflater.from(getContext());
        var dlgBind = DialogSliderBinding.inflate(inflater);
        dlgBind.slider.setMax(255);
        dlgBind.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dlgBind.label.setText(String.valueOf(progress));
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
        dlgBind.slider.setProgress(currentValue);
//        if (address != null) {
//            for (var device : mDevices) {
//                if (address.equals(device.getAddress())) {
//                    var characteristic = device.getCachedCharacteristic(characteristicUUID);
//                    var value = characteristic != null ? characteristic.getValue() : null;
//                    if (value != null && value.length > 0)
//                        dlgBind.slider.setProgress(value[0]);
//                    break;
//                }
//            }
//        }
        new AlertDialog.Builder(requireContext())
                .setView(dlgBind.getRoot())
                .setPositiveButton("Set", (dialog, which) -> {
                    var i = new Intent(BleSensorService.ACTION_SET_DATA);
                    i.putExtra(BleSensorService.EXTRA_ADDRESS, address);
                    i.putExtra(BleSensorService.EXTRA_DATA_UUID, characteristicUUID);
                    i.putExtra(BleSensorService.EXTRA_DATA, dlgBind.slider.getProgress());
                    sendIntentToService(i);
                })
                .show();
    }
}