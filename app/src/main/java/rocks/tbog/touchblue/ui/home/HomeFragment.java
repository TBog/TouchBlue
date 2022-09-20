package rocks.tbog.touchblue.ui.home;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import rocks.tbog.touchblue.AppViewModel;
import rocks.tbog.touchblue.BleListAdapter;
import rocks.tbog.touchblue.BluetoothLeService;
import rocks.tbog.touchblue.R;
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

        final var adapter = new BleListAdapter();
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
        var entry = ((BleListAdapter) binding.list.getAdapter()).getItem(info.position);
        menu.setHeaderTitle(entry.getAddress());
        menu.findItem(R.id.action_connect).setOnMenuItemClickListener(item -> {
            var i = new Intent(BluetoothLeService.ACTION_CONNECT_TO);
            i.putExtra(BluetoothLeService.EXTRA_ADDRESS, entry.getAddress());
            sendIntentToService(i);
            return true;
        });
        menu.findItem(R.id.action_debug).setOnMenuItemClickListener(item -> {
            var i = new Intent(BluetoothLeService.ACTION_CONNECT_TO);
            i.putExtra(BluetoothLeService.EXTRA_ADDRESS, entry.getAddress());
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
        i.setComponent(new ComponentName(ctx, BluetoothLeService.class));
        ContextCompat.startForegroundService(ctx, i);
    }

}