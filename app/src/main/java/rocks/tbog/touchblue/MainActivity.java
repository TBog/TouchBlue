package rocks.tbog.touchblue;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import java.util.Collections;

import rocks.tbog.touchblue.databinding.ActivityMainBinding;
import rocks.tbog.touchblue.helpers.BleHelper;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BL_main";

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    AppViewModel appData = null;
    private ActivityResultLauncher<Intent> connectToScannedDevice;

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
                R.id.nav_home, R.id.nav_services, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        connectToScannedDevice = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                var data = result.getData();
                var extras = data != null ? data.getExtras() : null;
                var scanResult = extras != null ? extras.getParcelable(ScanActivity.SCAN_RESULT) : null;
                if (scanResult instanceof ScanResult) {
                    var entry = new BleEntry((ScanResult) scanResult);

                    appData.setBleEntryList(Collections.singletonList(entry));
                    BleHelper.connect(getBaseContext(), entry);
                    binding.appBarMain.debugText.setText("Connecting to " + entry.getAddress());
                }
            }
        });

        appData = new ViewModelProvider(this).get(AppViewModel.class);
        appData.getBleEntryList().observe(this, bleEntries -> invalidateOptionsMenu());
        Log.i(TAG, appData.toString());
    }

    private void fabClick(View view) {
        connectToScannedDevice.launch(new Intent(getBaseContext(), ScanActivity.class));
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

        if (permissionReceived)
            binding.appBarMain.debugText.setText("Ready to scan for devices");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_reconnect).setOnMenuItemClickListener(item -> {
            var list = appData.getBleEntryList().getValue();
            if (list == null || list.isEmpty())
                return true;
            var entry = list.get(0);
            BleHelper.connect(getBaseContext(), entry);
            return true;
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        var list = appData.getBleEntryList().getValue();
        menu.findItem(R.id.action_reconnect).setVisible(list != null && !list.isEmpty());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}