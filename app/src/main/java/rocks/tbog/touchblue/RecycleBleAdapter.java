package rocks.tbog.touchblue;

import android.Manifest;
import android.bluetooth.le.ScanResult;
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

public class RecycleBleAdapter extends RecyclerView.Adapter<RecycleBleAdapter.Holder> {
    @NonNull
    private final ArrayList<ScanResult> list = new ArrayList<>();
    private OnItemClickListener mItemClickListener = null;

    interface OnItemClickListener {
        void onClick(ScanResult scanResult, int position);
    }

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
        var scanResult = list.get(position);
        holder.setContent(scanResult);
        holder.btnConnect.setOnClickListener(v -> {
            if (mItemClickListener != null)
                mItemClickListener.onClick(scanResult, position);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position < list.size() ? list.get(position).getDevice().getAddress().hashCode() : -1;
    }

    public void setItems(Collection<ScanResult> collection) {
        list.clear();
        list.addAll(collection);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
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

        public void setContent(ScanResult scanResult) {
//            if (scanResult == null) {
//                deviceName.setText("-");
//                deviceMac.setText("-");
//                deviceData.setText("-");
//                signal.setText("-");
//                btnConnect.setClickable(false);
//                return;
//            }
            var ctx = itemView.getContext();
            var scanRec = scanResult.getScanRecord();
            var dev = scanResult.getDevice();
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
                txPower = scanResult.getTxPower();
            }
            deviceName.setText(name);
            signal.setText(txPower + " dBm");
            deviceMac.setText(dev.getAddress());
            deviceData.setText("rssi " + scanResult.getRssi() + " dBm");
        }
    }
}
