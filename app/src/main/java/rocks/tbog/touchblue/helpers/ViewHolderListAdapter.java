package rocks.tbog.touchblue.helpers;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;

public abstract class ViewHolderListAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends ViewHolderAdapter<T, VH> {
    @NonNull
    protected final List<T> mList;

    protected ViewHolderListAdapter(@NonNull Class<? extends VH> viewHolderClass, int listItemLayout, @NonNull List<T> list) {
        super(viewHolderClass, listItemLayout);
        mList = list;
    }

    @LayoutRes
    protected int getItemViewTypeLayout(int viewType) {
        return mListItemLayout;
    }

    @Override
    public T getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    public void addItems(Collection<? extends T> items) {
        mList.addAll(items);
        notifyDataSetChanged();
    }

    public void addItem(T item) {
        mList.add(item);
        notifyDataSetChanged();
    }
}
