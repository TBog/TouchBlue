package rocks.tbog.touchblue.ui.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import rocks.tbog.touchblue.AppViewModel;
import rocks.tbog.touchblue.BleSensorService;
import rocks.tbog.touchblue.R;
import rocks.tbog.touchblue.databinding.DialogDropDownBinding;
import rocks.tbog.touchblue.databinding.DialogEditTextBinding;
import rocks.tbog.touchblue.databinding.FragmentDeviceSettingsBinding;
import rocks.tbog.touchblue.helpers.GattAttributes;

public class DeviceSettingsFragment extends Fragment {
    private static final String TAG = "DevSettings";
    private FragmentDeviceSettingsBinding binding;
    private AppViewModel appData;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DeviceSettingsViewModel deviceSettingsViewModel =
                new ViewModelProvider(this).get(DeviceSettingsViewModel.class);

        binding = FragmentDeviceSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDeviceSettings;
        deviceSettingsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        binding.list.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            var child = parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
            if (!(child instanceof HashMap))
                return false;
            @SuppressWarnings("unchecked")
            var childData = (HashMap<String, String>) child;
            var characteristicUUID = UUID.fromString(childData.get("characteristicUUID"));
            if (characteristicUUID == null)
                return false;
            var serviceUUID = appData.findServiceForCharacteristic(characteristicUUID);
            if (serviceUUID == null)
                return false;

            return true;
        });

        appData = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        appData.getUuidSet().observe(getViewLifecycleOwner(), serviceOrCharacteristicSet -> {
            var services = serviceOrCharacteristicSet.getServices();
            var groupData = new ArrayList<HashMap<String, String>>();
            var childData = new ArrayList<ArrayList<HashMap<String, String>>>();
            for (var service : services) {
                var group = new HashMap<String, String>();
                var children = new ArrayList<HashMap<String, String>>();
                groupData.add(group);
                childData.add(children);
                group.put("serviceUUID", service.toString());
                group.put("Name", GattAttributes.lookup(service, service.toString()));
                var characteristics = serviceOrCharacteristicSet.getCharacteristics(service);
                for (var characteristic : characteristics) {
                    var child = new HashMap<String, String>();
                    child.put("characteristicUUID", characteristic.toString());
                    child.put("Name", GattAttributes.lookup(characteristic, characteristic.toString()));
                    children.add(child);
                }
            }
            var adapter = new ExpandableListAdapter(
                    requireContext(),
                    groupData, android.R.layout.simple_expandable_list_item_2,
                    new String[]{"serviceUUID", "Name"}, new int[]{android.R.id.text2, android.R.id.text1},
                    childData, R.layout.expandable_list_child_characteristic,
                    new String[]{"characteristicUUID", "Name"}, new int[]{android.R.id.text2, android.R.id.text1},
                    new int[]{R.id.btn_download, R.id.btn_upload}
            );
            adapter.setOnButtonClickListener((buttonView, child) -> {
                final UUID characteristicUUID;
                final String deviceAddress;
                {
                    @SuppressWarnings("unchecked")
                    var map = (HashMap<String, String>) child;
                    characteristicUUID = UUID.fromString(map.get("characteristicUUID"));
                }
                {
                    @SuppressWarnings("unchecked")
                    var map = (HashMap<String, String>) binding.selectDevice.getSelectedItem();
                    deviceAddress = map.get("deviceAddress");
                }
                if (deviceAddress == null || !BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                    Log.e(TAG, "No sensor selected! `" + deviceAddress + "` is invalid");
                    return;
                }
                if (buttonView.getId() == R.id.btn_download) {
                    var i = new Intent(BleSensorService.ACTION_REQUEST_DATA);
                    i.putExtra(BleSensorService.EXTRA_ADDRESS, deviceAddress);
                    i.putExtra(BleSensorService.EXTRA_DATA_UUID, characteristicUUID);
                    sendIntentToService(i);
                } else if (buttonView.getId() == R.id.btn_upload) {
                    showUploadDialog(deviceAddress, characteristicUUID);
                }
            });
            binding.list.setAdapter(adapter);
        });
        appData.getBleEntryList().observe(getViewLifecycleOwner(), bleSensors -> {
            var data = new ArrayList<HashMap<String, String>>();
            for (var sensor : bleSensors) {
                var sensorData = new HashMap<String, String>();
                data.add(sensorData);
                sensorData.put("deviceAddress", sensor.getAddress());
                sensorData.put("Name", sensor.getName());
            }
            binding.selectDevice.setAdapter(new SimpleAdapter(
                    requireContext(),
                    data, android.R.layout.simple_list_item_2,
                    new String[]{"deviceAddress", "Name"}, new int[]{android.R.id.text2, android.R.id.text1}
            ));
        });

        return root;
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

    private void showUploadDialog(@NonNull String address, @NonNull UUID characteristic) {
        if (GattAttributes.ACCEL_RANGE.equals(characteristic)) {
            showDropDownDialog(address, characteristic, R.array.accel_range_entries, R.array.accel_range_values);
        } else if (GattAttributes.ACCEL_BANDWIDTH.equals(characteristic)) {
            showDropDownDialog(address, characteristic, R.array.accel_bandwidth_entries, R.array.accel_bandwidth_values);
        } else if (GattAttributes.ACCEL_SAMPLE_RATE.equals(characteristic)) {
            showDropDownDialog(address, characteristic, R.array.accel_sample_rate_entries, R.array.accel_sample_rate_values);
        } else {
            showByteDialog(address, characteristic);
        }
    }

    private void showByteDialog(String address, UUID characteristic) {
        var inflater = LayoutInflater.from(getContext());
        var dlgBind = DialogEditTextBinding.inflate(inflater);
        dlgBind.editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        {
            var dev = appData.findSensor(address);
            var data = dev != null ? dev.getDataValue(characteristic) : null;
            if (data != null)
                dlgBind.editText.setText(String.valueOf(data));
        }
        new AlertDialog.Builder(requireContext())
                .setView(dlgBind.getRoot())
                .setPositiveButton("Set", (dialog, which) -> {
                    var value = dlgBind.editText.getText().toString();
                    int newValue;
                    try {
                        newValue = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "value `" + value + "` is not integer");
                        Toast.makeText(getContext(), "value `" + value + "` is not integer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    var i = new Intent(BleSensorService.ACTION_SET_DATA);
                    i.putExtra(BleSensorService.EXTRA_ADDRESS, address);
                    i.putExtra(BleSensorService.EXTRA_DATA_UUID, characteristic);
                    i.putExtra(BleSensorService.EXTRA_DATA, newValue);
                    sendIntentToService(i);
                })
                .show();
    }

    private void showDropDownDialog(String address, UUID characteristic, @ArrayRes int entries, @ArrayRes int values) {
        var inflater = LayoutInflater.from(getContext());
        var dlgBind = DialogDropDownBinding.inflate(inflater);
        var adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(entries));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dlgBind.dropDown.setAdapter(adapter);
//        dlgBind.dropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // nothing to do
//            }
//        });
        new AlertDialog.Builder(requireContext())
                .setView(dlgBind.getRoot())
                .setPositiveButton("Set", (dialog, which) -> {
                    var arrValues = getResources().getStringArray(values);
                    var selectedPosition = dlgBind.dropDown.getSelectedItemPosition();
                    var value = selectedPosition < arrValues.length ? arrValues[selectedPosition] : null;

                    int newValue;
                    try {
                        newValue = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "value `" + value + "` is not integer");
                        return;
                    }

                    var i = new Intent(BleSensorService.ACTION_SET_DATA);
                    i.putExtra(BleSensorService.EXTRA_ADDRESS, address);
                    i.putExtra(BleSensorService.EXTRA_DATA_UUID, characteristic);
                    i.putExtra(BleSensorService.EXTRA_DATA, newValue);
                    sendIntentToService(i);
                })
                .show();
    }
}