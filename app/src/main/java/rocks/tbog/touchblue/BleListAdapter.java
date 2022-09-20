package rocks.tbog.touchblue;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.touchblue.helpers.ViewHolderAdapter;
import rocks.tbog.touchblue.helpers.ViewHolderListAdapter;

public class BleListAdapter extends ViewHolderListAdapter<BleEntry, BleListAdapter.ViewHolder> {

    public BleListAdapter() {
        super(ViewHolder.class, android.R.layout.simple_list_item_2, new ArrayList<>());
    }

    public void setList(List<BleEntry> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends ViewHolderAdapter.ViewHolder<BleEntry> {
        TextView text1;
        TextView text2;

        protected ViewHolder(View view) {
            super(view);
            text1 = view.findViewById(android.R.id.text1);
            text2 = view.findViewById(android.R.id.text2);
        }

        @Override
        protected void setContent(BleEntry content, int position, @NonNull ViewHolderAdapter<BleEntry, ? extends ViewHolderAdapter.ViewHolder<BleEntry>> adapter) {
            text1.setText(content.getAddress());
            text2.setText(content.getName() + (content.isConnected() ? " connected" : ""));
        }
    }
}
