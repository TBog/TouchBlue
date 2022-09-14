package rocks.tbog.touchblue.ui.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import rocks.tbog.touchblue.AppViewModel;
import rocks.tbog.touchblue.R;
import rocks.tbog.touchblue.databinding.FragmentServicesBinding;

public class ServicesFragment extends Fragment {

    private FragmentServicesBinding binding;
    private AppViewModel appData;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ServicesViewModel servicesViewModel = new ViewModelProvider(this).get(ServicesViewModel.class);

        binding = FragmentServicesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textGallery;
        servicesViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        appData = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        Log.i("Gallery", appData.toString());
        appData.getBleEntryList().observe(getViewLifecycleOwner(), collection -> {
            Log.d("Gallery", "entry list changed. New size is " + collection.size());
            displayGattServices(collection.get(0).services);
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // Populate the data structure that is bound to the ExpandableListView on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        final String LIST_NAME = "NAME";
        final String LIST_UUID = "UUID";
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final int count = gattServices.size();
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>(count);
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>(count);

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>(2);
            {
                var uuid = gattService.getUuid().toString();
                currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
            }
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                var prop = gattCharacteristic.getProperties();
                HashMap<String, String> currentCharaData = new HashMap<>(2);
                {
                    var uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                }
                gattCharacteristicGroupData.add(currentCharaData);
            }
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                getContext(),
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        binding.list.setAdapter(gattServiceAdapter);
        binding.list.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            var service = gattServiceData.get(groupPosition).get(LIST_UUID);
            var characteristic = gattCharacteristicData.get(groupPosition).get(childPosition).get(LIST_UUID);
            onCharacteristicClick(service, characteristic);
            return true;
        });
    }

    private void onCharacteristicClick(String service, String characteristic) {
        var list = appData.getBleEntryList().getValue();
        if (list == null || list.isEmpty())
            return;
        var entry = list.get(0);
        var serviceUUID = UUID.fromString(service);
        var characteristicUUID = UUID.fromString(characteristic);
        var gattCharacteristic = entry.getCharacteristic(serviceUUID, characteristicUUID);
        if (gattCharacteristic == null)
            return;
        // if we can write
        if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            var value = gattCharacteristic.getValue();
            if (value == null || value.length == 0)
                value = new byte[]{0};
            if (value[0] == 0)
                entry.writeCharacteristic(serviceUUID, characteristicUUID, 0x1);
            else
                entry.writeCharacteristic(serviceUUID, characteristicUUID, 0x0);
        } else if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            var text = gattCharacteristic.getStringValue(0);
            if (text == null) {
                Toast.makeText(getContext(), "Value not read yet. Reading now...", Toast.LENGTH_SHORT).show();
                entry.readCharacteristic(serviceUUID, characteristicUUID);
            } else {
                Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "can't read or write", Toast.LENGTH_SHORT).show();
        }
    }
}