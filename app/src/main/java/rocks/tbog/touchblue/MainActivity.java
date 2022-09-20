package rocks.tbog.touchblue;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import rocks.tbog.touchblue.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BL_main";

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> connectToScannedDevice;
    private AppViewModel appData;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final var action = intent.getAction();
            Log.i(TAG, "onReceive action=" + action);
            final var extras = intent.getExtras();
            executeAction(action, extras);
        }
    };

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
                R.id.nav_home, R.id.nav_slideshow)
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
                    var address = ((ScanResult) scanResult).getDevice().getAddress();
                    appData.addConnection((ScanResult) scanResult);
                    sendConnectAction(address);
//                    var entry = new BleEntry((ScanResult) scanResult);
//
//                    appData.setBleEntryList(Collections.singletonList(entry));
//                    BleHelper.connect(getBaseContext(), entry);
//                    binding.appBarMain.debugText.setText("Connecting to " + entry.getAddress());
                }
            }
        });

        appData = new ViewModelProvider(this).get(AppViewModel.class);
        appData.getBleEntryList().observe(this, bleEntries -> invalidateOptionsMenu());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_STOP_SERVICE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void sendConnectAction(String address) {
        var i = new Intent(BluetoothLeService.ACTION_CONNECT_TO);
        i.putExtra(BluetoothLeService.EXTRA_ADDRESS, address);
        sendIntentToService(i);
    }

    private void sendIntentToService(@NonNull final Intent intent) {
//        if (foregroundServiceRunning()) {
//            sendBroadcast(intent);
//        } else
        {
            var ctx = getBaseContext();
            var i = new Intent(intent);
            i.setComponent(new ComponentName(ctx, BluetoothLeService.class));
            ContextCompat.startForegroundService(ctx, i);
        }
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
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

        var action = getIntent() != null ? getIntent().getAction() : null;
        if (BluetoothLeService.ACTION_STOP_SERVICE.equals(action)) {
            if (foregroundServiceRunning()) {
                Log.i(TAG, "stop service");
                var ctx = getBaseContext();
                var i = new Intent(ctx, BluetoothLeService.class);
                i.setAction(BluetoothLeService.ACTION_STOP_SERVICE);
                ctx.stopService(i);
            }
        } else {
            if (foregroundServiceRunning()) {
                Log.i(TAG, "service already running");
            } else {
                var ctx = getBaseContext();
                var i = new Intent(ctx, BluetoothLeService.class);
                i.setAction(BluetoothLeService.ACTION_START_SERVICE);
                ContextCompat.startForegroundService(ctx, i);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_start_service).setOnMenuItemClickListener(item -> {
            if (!foregroundServiceRunning()) {
                var ctx = getBaseContext();
                var i = new Intent(ctx, BluetoothLeService.class);
                i.setAction(BluetoothLeService.ACTION_START_SERVICE);
                ContextCompat.startForegroundService(ctx, i);
            }
            return true;
        });
        menu.findItem(R.id.action_stop_service).setOnMenuItemClickListener(item -> {
            var ctx = getBaseContext();
            var i = new Intent(ctx, BluetoothLeService.class);
            i.setAction(BluetoothLeService.ACTION_STOP_SERVICE);
            ctx.stopService(i);
            return true;
        });
        menu.findItem(R.id.action_test).setOnMenuItemClickListener(item -> {
            sendConnectAction("20:E4:63:15:BD:03");
            return true;
        }).setTitle("20:E4:63:15:BD:03");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean serviceRunning = foregroundServiceRunning();
        menu.findItem(R.id.action_start_service).setVisible(!serviceRunning);
        menu.findItem(R.id.action_stop_service).setVisible(serviceRunning);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public boolean foregroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (var service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (BluetoothLeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void fabClick(View view) {
        connectToScannedDevice.launch(new Intent(getBaseContext(), ScanActivity.class));
    }

    private void executeAction(@Nullable String action, @Nullable Bundle extras) {
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            String address = extras != null ? extras.getString(BluetoothLeService.EXTRA_ADDRESS) : null;
            var entry = appData.addConnection(address);
            entry.setConnected(true);
        }
        if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            String address = extras != null ? extras.getString(BluetoothLeService.EXTRA_ADDRESS) : null;
            var entry = appData.addConnection(address);
            entry.setConnected(false);
        }
        if (BluetoothLeService.ACTION_STOP_SERVICE.equals(action)) {
            var ctx = getBaseContext();
            var i = new Intent(ctx, BluetoothLeService.class);
            i.setAction(BluetoothLeService.ACTION_STOP_SERVICE);
            ctx.stopService(i);
        }
    }
}