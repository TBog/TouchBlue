package rocks.tbog.touchblue.ui.game;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.touchblue.R;

public class GameActionAdapter extends BaseExpandableListAdapter {
    private final List<GameViewModel.GameAction> mGameActionList = new ArrayList<>(0);
    protected final int[] mButtonIds;
    @LayoutRes
    private final int mExpandedGroupLayout;
    @LayoutRes
    private final int mCollapsedGroupLayout;
    @LayoutRes
    private final int mChildLayout;
    @LayoutRes
    private final int mLastChildLayout;

    protected OnButtonClick mButtonListener = null;

    interface OnButtonClick {
        void onButtonClick(View buttonView, GameActionAdapter adapter, int groupPosition, int childPosition);
    }

    public GameActionAdapter(int[] buttons) {
        mButtonIds = buttons;
        mExpandedGroupLayout = android.R.layout.simple_expandable_list_item_1;
        mCollapsedGroupLayout = android.R.layout.simple_expandable_list_item_2;
        mChildLayout = R.layout.expandable_list_child_game_action;
        mLastChildLayout = R.layout.expandable_list_child_game_action;
    }

    public void setGameActionList(@NonNull List<GameViewModel.GameAction> list) {
        mGameActionList.clear();
        mGameActionList.addAll(list);
        notifyDataSetInvalidated();
    }

    @NonNull
    public List<GameViewModel.GameAction> getGameActionList() {
        return mGameActionList;
    }

    public void changeActionValue(int groupPosition, int value) {
        var action = mGameActionList.get(groupPosition);
        action.mValue = value;
        notifyDataSetInvalidated();
    }

    public void changeAction(int groupPosition, GameViewModel.GameAction action) {
        mGameActionList.set(groupPosition, action);
        notifyDataSetInvalidated();
    }

    public void setOnButtonClickListener(@Nullable OnButtonClick listener) {
        mButtonListener = listener;
    }

    @Override
    public int getGroupCount() {
        return mGameActionList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 2;
    }

    @Override
    public GameViewModel.GameAction getGroup(int groupPosition) {
        return mGameActionList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        var action = mGameActionList.get(groupPosition);
        switch (childPosition) {
            case 0:
                return action.mAction;
            case 1:
                return action.mValue;
        }
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return getGroup(groupPosition).hashCode();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        var child = getChild(groupPosition, childPosition);
        if (child == null)
            return 0;
        return child.hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newGroupView(isExpanded, parent);
        } else {
            v = convertView;
        }
        var group = getGroup(groupPosition);
        if (isExpanded)
            bindView(v, new Object[]{group, group.mAction}, new int[]{android.R.id.text1, android.R.id.text2});
        else
            bindView(v, new Object[]{group.mAction, group.mValue}, new int[]{android.R.id.text1, android.R.id.text2});
        return v;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        View childView;
        if (convertView == null) {
            childView = newChildView(isLastChild, parent);
        } else {
            childView = convertView;
        }
        var child = getChild(groupPosition, childPosition);

        for (int buttonId : mButtonIds) {
            var button = childView.findViewById(buttonId);
            if (button != null)
                button.setOnClickListener(buttonView -> buttonClick(buttonView, groupPosition, childPosition));
        }

        bindView(childView, new Object[]{child}, new int[]{android.R.id.text1});
        return childView;
    }

    protected void buttonClick(View buttonView, int groupPosition, int childPosition) {
        if (mButtonListener != null) {
            mButtonListener.onButtonClick(buttonView, this, groupPosition, childPosition);
        }
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public View newGroupView(boolean isExpanded, ViewGroup parent) {
        var mInflater = LayoutInflater.from(parent.getContext());
        return mInflater.inflate((isExpanded) ? mExpandedGroupLayout : mCollapsedGroupLayout,
                parent, false);
    }

    public View newChildView(boolean isLastChild, ViewGroup parent) {
        var mInflater = LayoutInflater.from(parent.getContext());
        return mInflater.inflate((isLastChild) ? mLastChildLayout : mChildLayout, parent, false);
    }

    private void bindView(View view, Object[] data, int[] viewIds) {
        int len = viewIds.length;

        for (int i = 0; i < len; i++) {
            View v = view.findViewById(viewIds[i]);
            if (v instanceof TextView) {
                ((TextView) v).setText(String.valueOf(data[i]));
            }
        }
    }

}
