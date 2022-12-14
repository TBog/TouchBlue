package rocks.tbog.touchblue.ui.home;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.touchblue.BleSensorModel;
import rocks.tbog.touchblue.helpers.GattAttributes;
import rocks.tbog.touchblue.helpers.ViewHolderAdapter;
import rocks.tbog.touchblue.helpers.ViewHolderListAdapter;

public class BleSensorAdapter extends ViewHolderListAdapter<BleSensorModel, BleSensorAdapter.ViewHolder> {

    public BleSensorAdapter() {
        super(ViewHolder.class, android.R.layout.simple_list_item_2, new ArrayList<>());
    }

    public void setList(List<BleSensorModel> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends ViewHolderAdapter.ViewHolder<BleSensorModel> {
        TextView text1;
        TextView text2;

        protected ViewHolder(View view) {
            super(view);
            text1 = view.findViewById(android.R.id.text1);
            text2 = view.findViewById(android.R.id.text2);
        }

        @Override
        protected void setContent(BleSensorModel content, int position, @NonNull ViewHolderAdapter<BleSensorModel, ? extends ViewHolderAdapter.ViewHolder<BleSensorModel>> adapter) {
            text1.setText(content.getAddress() + (content.isConnected() ? " connected" : ""));
            text2.setText(content.getName() + " " + content.getDataValue(GattAttributes.LED_SWITCH));
        }
    }
}
