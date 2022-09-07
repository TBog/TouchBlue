package rocks.tbog.touchblue;

import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import rocks.tbog.touchblue.helpers.ViewHolderAdapter;
import rocks.tbog.touchblue.helpers.ViewHolderListAdapter;

public class BleListAdapter extends ViewHolderListAdapter<BleEntry, BleListAdapter.ViewHolder> {

    public BleListAdapter() {
        super(ViewHolder.class, android.R.layout.simple_list_item_1, new ArrayList<>());
    }

    public static class ViewHolder extends ViewHolderAdapter.ViewHolder<BleEntry> {

        protected ViewHolder(View view) {
            super(view);
        }

        @Override
        protected void setContent(BleEntry content, int position, @NonNull ViewHolderAdapter<BleEntry, ? extends ViewHolderAdapter.ViewHolder<BleEntry>> adapter) {

        }
    }
}
