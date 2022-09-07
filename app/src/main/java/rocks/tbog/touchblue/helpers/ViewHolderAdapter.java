package rocks.tbog.touchblue.helpers;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.touchblue.BuildConfig;

/**
 * Adapter class that implements the View holder pattern.
 * The ViewHolder is held as a tag in the list item view.
 *
 * @param <T>  Type of data to send to the ViewHolder
 * @param <VH> ViewHolder class
 */
public abstract class ViewHolderAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends BaseAdapter {
    @NonNull
    final Class<? extends VH> mViewHolderClass;
    @LayoutRes
    final int mListItemLayout;

    protected ViewHolderAdapter(@NonNull Class<? extends VH> viewHolderClass, @LayoutRes int listItemLayout) {
        mViewHolderClass = viewHolderClass;
        mListItemLayout = listItemLayout;
    }

    @LayoutRes
    protected int getItemViewTypeLayout(int viewType) {
        return mListItemLayout;
    }

    @Override
    public abstract T getItem(int position);

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Nullable
    protected VH getNewViewHolder(View view) {
        VH holder = null;
        try {
            holder = mViewHolderClass.getDeclaredConstructor(View.class).newInstance(view);
        } catch (Exception e) {
            Log.e("VHA", "ViewHolder can't be instantiated (make sure class and constructor are public)", e);
        }
        return holder;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            int viewType = getItemViewType(position);
            if (BuildConfig.DEBUG) {
                int viewTypeCount = getViewTypeCount();
                if (viewType >= viewTypeCount)
                    throw new IllegalStateException("ViewType " + viewType + " >= ViewTypeCount " + viewTypeCount);
            }
            @LayoutRes
            int itemLayout = getItemViewTypeLayout(viewType);
            view = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        } else {
            view = convertView;
        }

        Object tag = view.getTag();
        VH holder = mViewHolderClass.isInstance(tag) ? mViewHolderClass.cast(tag) : getNewViewHolder(view);
        if (holder != null) {
            T content = getItem(position);
            holder.setContent(content, position, this);
        }
        return view;

    }

    public static abstract class ViewHolder<T> {
        protected ViewHolder(View view) {
            view.setTag(this);
        }

        protected abstract void setContent(T content, int position, @NonNull ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter);
    }
}
