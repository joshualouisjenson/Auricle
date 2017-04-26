package ku.eecscap.team8.auricle;

import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Austin Kurtti on 4/23/2017.
 * Last Edited by Austin Kurtti on 4/25/2017
 */

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ViewHolder> {

    private ListingHelper[] mDataSet;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLength, tvDateCreated;
        public ViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.listing_item_title);
            tvLength = (TextView) view.findViewById(R.id.listing_item_length);
            tvDateCreated = (TextView) view.findViewById(R.id.listing_item_date_created);
        }
    }

    public ListingAdapter(ListingHelper[] dataSet) {
        mDataSet = dataSet;
    }

    @Override
    public ListingAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final int listingId = mDataSet[position].getId();
        final String title = mDataSet[position].getTitle();
        final String length = mDataSet[position].getLength();
        final String dateCreated = mDataSet[position].getDateCreated();

        // set listener for tapping on listing item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: playback popup
                Snackbar.make(view, "Clicked on: " + title, Snackbar.LENGTH_SHORT).show();
            }
        });

        holder.tvTitle.setTag(R.integer.tag_key_listing_id, listingId);
        holder.tvTitle.setText(title);
        holder.tvLength.setText(length);
        holder.tvDateCreated.setText(dateCreated);
    }

    @Override
    public int getItemCount() {
        return mDataSet.length;
    }

    public void clearDataSet() {
        mDataSet = new ListingHelper[0];
    }

    public void fillDataSet(ListingHelper[] newSet) {
        mDataSet = newSet;
    }
}
