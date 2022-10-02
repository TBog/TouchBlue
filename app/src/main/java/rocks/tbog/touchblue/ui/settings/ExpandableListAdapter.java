package rocks.tbog.touchblue.ui.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleExpandableListAdapter;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class ExpandableListAdapter extends SimpleExpandableListAdapter {

    protected final int[] mButtonIds;
    protected OnButtonClick mButtonListener = null;

    interface OnButtonClick {
        void onButtonClick(View buttonView, Object child);
    }

    public ExpandableListAdapter(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo, int[] buttons) {
        super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
        mButtonIds = buttons;
    }

    public void setOnButtonClickListener(@Nullable OnButtonClick listener) {
        mButtonListener = listener;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View childView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
        for (int buttonId : mButtonIds) {
            childView
                    .findViewById(buttonId)
                    .setOnClickListener(buttonView -> buttonClick(buttonView, groupPosition, childPosition));
        }
        return childView;
    }

    protected void buttonClick(View buttonView, int groupPosition, int childPosition) {
        if (mButtonListener != null) {
            mButtonListener.onButtonClick(buttonView, getChild(groupPosition, childPosition));
        }
    }
}
