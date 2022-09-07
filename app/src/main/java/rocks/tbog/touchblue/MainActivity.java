package rocks.tbog.touchblue;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import rocks.tbog.touchblue.databinding.ActivityMainBinding;
import rocks.tbog.touchblue.helpers.BleHelper;
import rocks.tbog.touchblue.helpers.BleScan;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BL_main";

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> promptEnableBluetooth;
    private ActivityResultLauncher<String[]> requestMultiplePermissions;
    private final MutableLiveData<Boolean> bluetoothPermissionReceived = new MutableLiveData<>();
    private boolean bluetoothPermissionAsked = false;
    private BleScan mScan = null;
    AppViewModel appData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(this::fabClick);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        promptEnableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            bluetoothPermissionReceived.setValue(result.getResultCode() == RESULT_OK);
        });

        requestMultiplePermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allReceived = true;
            for (var entry : result.entrySet()) {
                Log.i(TAG, entry.getKey() + " " + entry.getValue());
                if (!entry.getValue()) {
                    allReceived = false;
                }
            }
            bluetoothPermissionReceived.setValue(allReceived);
        });

        bluetoothPermissionReceived.observe(this, bltEnabled -> {
            binding.appBarMain.debugText.setText(bltEnabled ? "BLT enabled" : "Need BLT permission");
        });

        if (savedInstanceState != null) {
            bluetoothPermissionAsked = savedInstanceState.getBoolean("bluetoothPermissionAsked");
        }

        appData = new ViewModelProvider(this).get(AppViewModel.class);
        Log.i(TAG, appData.toString());
    }

    private void fabClick(View view) {
        boolean isScanning = mScan != null && mScan.isScanning();
        if (isScanning) {
            // stop scanning
            mScan.stopScan(getBaseContext());
            // set FAB to search icon
            binding.appBarMain.fab.setImageResource(R.drawable.ic_search);
            return;
        }

        // start scanning
        if (startBleScan(getBaseContext())) {
            // set FAB to off icon
            binding.appBarMain.fab.setImageResource(R.drawable.ic_search_off);
        }
    }

    private boolean startBleScan(Context ctx) {
        if (mScan != null)
            mScan.stopScan(ctx);
        try {
            mScan = BleHelper.startScan2(ctx);
        } catch (Exception e) {
            Log.e(TAG, "start scan", e);
            return false;
        }
        mScan.getScanList().observe(this, bleEntries -> appData.setBleEntryList(bleEntries));
        return mScan.isScanning();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("bluetoothPermissionAsked", bluetoothPermissionAsked);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean blt_le = getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE);
        binding.appBarMain.debugText.setText(blt_le ? R.string.app_name : R.string.error_no_blt_le);
        if (blt_le) {
            BluetoothManager bltMgr = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bltMgr.getAdapter().isEnabled()) {
                binding.appBarMain.debugText.setText("Bluetooth in turned on");
            } else {
                binding.appBarMain.debugText.setText("Please turn on bluetooth");
            }
        }
        boolean permissionReceived;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionReceived = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            permissionReceived |= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            permissionReceived = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
        bluetoothPermissionReceived.setValue(permissionReceived);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // bluetoothPermissionReceived may have null value (uninitialized at startup)
        if (!bluetoothPermissionAsked) {
            askForBluetoothPermission();
        }
    }

    private void askForBluetoothPermission() {
        bluetoothPermissionAsked = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT});
        } else {
            promptEnableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_settings).setOnMenuItemClickListener(item -> {
            askForBluetoothPermission();
            return true;
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}