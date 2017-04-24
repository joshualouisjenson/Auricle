package ku.eecscap.team8.auricle;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Austin Kurtti on 4/23/2017.
 */

public class ListingFragment extends Fragment {

    private static final String TAG = "ListingFragment";
    private static final int SPAN_COUNT = 2;

    protected DBHelper dbHelper;

    private enum LayoutManagerType {
        GRID,
        LINEAR
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected ListingAdapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ListingHelper[] mDataSet;

    @Override
    public void onResume() {
        ListingHelper[] refreshedItems = buildDataSet();
        mAdapter.clearDataSet();
        mAdapter.fillDataSet(refreshedItems);
        mAdapter.notifyDataSetChanged();

        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(getContext());
        mDataSet = buildDataSet();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_listing, container, false);
        rootView.setTag(TAG); // probably don't need to do this

        // init layout variables
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_listing);
//        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mCurrentLayoutManagerType = LayoutManagerType.LINEAR;
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        mAdapter = new ListingAdapter(mDataSet);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // set scroll position
        if(mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }

        // set layout; currently only using linear, for now
        switch(layoutManagerType) {
            case GRID:
                mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
                mCurrentLayoutManagerType = LayoutManagerType.GRID;
                break;

            case LINEAR:
            default:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    public ListingHelper[] buildDataSet() {
        // query db for listing data
        Cursor allItems = dbHelper.getListingData();
        ListingHelper[] dataSet = new ListingHelper[allItems.getCount()];
        if(allItems.getCount() > 0) {
            int position;
            while(!allItems.isAfterLast()) {
                position = allItems.getPosition();
                dataSet[position] = new ListingHelper();
                dataSet[position].setId(allItems.getInt(allItems.getColumnIndex(DBHelper.LISTING_COLUMN_LISTING_ID)));
                dataSet[position].setTitle(allItems.getString(allItems.getColumnIndex(DBHelper.LISTING_COLUMN_FILENAME)));
                allItems.moveToNext();
            }
        }

        return dataSet;
    }
}