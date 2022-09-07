package rocks.tbog.touchblue.ui.gallery;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Collections;

import rocks.tbog.touchblue.AppViewModel;
import rocks.tbog.touchblue.BleEntry;
import rocks.tbog.touchblue.RecycleBleAdapter;
import rocks.tbog.touchblue.databinding.FragmentGalleryBinding;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textGallery;
        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        final RecycleBleAdapter bleAdapter = new RecycleBleAdapter();
        binding.list.setAdapter(bleAdapter);

        bleAdapter.setItems(Collections.singletonList(new BleEntry()));
        //bleAdapter.setOnItemClickListener(item->{});

        var appData = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        Log.i("Gallery", appData.toString());
        appData.getBleEntryList().observe(getViewLifecycleOwner(), collection -> {
            Log.d("Gallery", "entry list changed. New size is " + collection.size());
            bleAdapter.setItems(collection);
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}