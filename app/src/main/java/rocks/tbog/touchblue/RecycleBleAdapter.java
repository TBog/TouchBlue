package rocks.tbog.touchblue;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.touchblue.helpers.BleHelper;

public class RecycleBleAdapter extends RecyclerView.Adapter<RecycleBleAdapter.Holder> {
    @NonNull
    private final ArrayList<BleEntry> list = new ArrayList<>();

    public RecycleBleAdapter() {
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.result_item, parent, false);
        return new Holder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.setContent(list.get(position));
    }

    public void setItems(Collection<BleEntry> collection) {
        list.clear();
        list.addAll(collection);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position < list.size() ? list.get(position).hashCode() : -1;
    }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceMac;
        TextView deviceData;
        TextView signal;
        ImageButton btnConnect;

        public Holder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceMac = itemView.findViewById(R.id.device_mac);
            deviceData = itemView.findViewById(R.id.device_data);
            signal = itemView.findViewById(R.id.signal);
            btnConnect = itemView.findViewById(R.id.btn_connect);
        }

        public void setContent(BleEntry entry) {
            if (entry.scanResult == null) {
                deviceName.setText("-");
                deviceMac.setText("-");
                deviceData.setText("-");
                signal.setText("-");
                btnConnect.setClickable(false);
                return;
            }
            var ctx = itemView.getContext();
            var scanRec = entry.scanResult.getScanRecord();
            var dev = entry.scanResult.getDevice();
            String name = scanRec.getDeviceName();
            if (name == null || name.isEmpty()) {
                if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    name = dev.getName();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (name == null || name.isEmpty()) {
                    name = dev.getAlias();
                }
            }
            if (name == null || name.isEmpty()) {
                name = "-";
            }
            int txPower = scanRec.getTxPowerLevel();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                txPower = entry.scanResult.getTxPower();
            }
            deviceName.setText(name);
            signal.setText(txPower + " dBm");
            deviceMac.setText(dev.getAddress());
            deviceData.setText("rssi " + entry.scanResult.getRssi() + " dBm");
            btnConnect.setOnClickListener(v -> {
                BleHelper.connect(ctx, entry.scanResult);
            });
        }
    }
}
