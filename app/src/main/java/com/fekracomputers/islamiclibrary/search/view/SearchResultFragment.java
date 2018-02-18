package com.fekracomputers.islamiclibrary.search.view;

import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fekracomputers.islamiclibrary.R;
import com.fekracomputers.islamiclibrary.search.BookSearcher;
import com.fekracomputers.islamiclibrary.search.model.BookSearchResultsContainer;
import com.fekracomputers.islamiclibrary.search.model.SearchOptions;
import com.fekracomputers.islamiclibrary.search.model.SearchRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnSearchResultFragmentInteractionListener}
 * interface.
 */
public class SearchResultFragment extends Fragment implements
        SearchResultRecyclerViewAdapter.SearchResultOnClickDelegateListener {
    public static final String ARG_SEARCHABLE_BOOKS = "searchable_books";
    // TODO: Customize parameter argument names
    public static final String ARG_IS_GLOBAL_SEARCH = "is_global_search";
    SearchRequest mSearchRequest;
    @NonNull
    List<BookSearchResultsContainer> bookSearchResultsContainerList = new ArrayList<>();
    // TODO: Customize parameters
    @Nullable
    private OnSearchResultFragmentInteractionListener mListener;
    private boolean mIsGlobalSearch;
    private ProgressBar mProgressBar;
    private TextView mTotalBooksTextView;
    private TextView mNumberOfAlreadySearchedBooksTextView;
    @Nullable
    private SearchResultRecyclerViewAdapter searchResultRecyclerViewAdapter;
    @Nullable
    private ArrayList<Integer> requestedSearchBookIds;
    @Nullable
    private String mSearchQuery;
    private SearchRequest searchRequest;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SearchResultFragment() {
    }

    @NonNull
    public static Fragment newInstance(Bundle searchIntentBundle) {

        SearchResultFragment fragment = new SearchResultFragment();
        fragment.setArguments(searchIntentBundle);
        return fragment;

    }

    @NonNull
    public static SearchResultFragment newInstance(boolean isGlobalSearch, ArrayList<Integer> searchableBooksIds) {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_GLOBAL_SEARCH, isGlobalSearch);
        args.putIntegerArrayList(ARG_SEARCHABLE_BOOKS, searchableBooksIds);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

            mSearchQuery = getArguments().getString(SearchManager.QUERY);
            mIsGlobalSearch = getArguments().getBoolean(ARG_IS_GLOBAL_SEARCH);
            requestedSearchBookIds = getArguments().getIntegerArrayList(ARG_SEARCHABLE_BOOKS);
        }
        searchRequest = new SearchRequest(mSearchQuery, new SearchOptions(), requestedSearchBookIds, !mIsGlobalSearch);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result_list, container, false);


        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchRequest = new SearchRequest(mSearchQuery, new SearchOptions(), requestedSearchBookIds, !mIsGlobalSearch);
        new SearchAsyncTask().execute(searchRequest);
        searchResultRecyclerViewAdapter = new SearchResultRecyclerViewAdapter(bookSearchResultsContainerList, this, getContext());

        recyclerView.setAdapter(searchResultRecyclerViewAdapter);
        mProgressBar = view.findViewById(R.id.search_progress);
        mProgressBar.setMax(requestedSearchBookIds.size());
        mTotalBooksTextView = view.findViewById(R.id.total_books);
        mTotalBooksTextView.setText(String.valueOf(requestedSearchBookIds.size()));
        mNumberOfAlreadySearchedBooksTextView = view.findViewById(R.id.current_book);
        mNumberOfAlreadySearchedBooksTextView.setText("0");
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSearchResultFragmentInteractionListener) {
            mListener = (OnSearchResultFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSearchResultFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onSearchResultClicked(int parentAdapterPosition, int childAdapterPosition) {
        mListener.onSearchResultClicked(bookSearchResultsContainerList.get(parentAdapterPosition), childAdapterPosition, searchRequest);
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSearchResultFragmentInteractionListener {
        void onSearchResultClicked(BookSearchResultsContainer bookSearchResultsContainer, int childAdapterPosition, SearchRequest searchRequest);
    }

    private class SearchAsyncTask extends AsyncTask<SearchRequest, BookSearchResultsContainer, Void> {


        private int mBooksSearched = 0;
        private int mResults = 0;

        @Override
        protected void onPreExecute() {
        }

        @Nullable
        @Override
        protected Void doInBackground(SearchRequest... searchRequests) {
            BookSearcher bookSearcher = new BookSearcher(SearchResultFragment.this.getContext(), searchRequests[0]);
            ArrayList<Integer> searchableBooksIds = searchRequests[0].getSearchBleBooksId();
            for (Integer bookId : searchableBooksIds) {
                publishProgress(bookSearcher.getBookSearchResultsContainer(bookId));
            }
            return null;
        }


        @Override
        protected void onProgressUpdate(BookSearchResultsContainer... values) {
            if (values[0].getChildCount() != 0) {
                bookSearchResultsContainerList.add(values[0]);
                searchResultRecyclerViewAdapter.notifyParentInserted(bookSearchResultsContainerList.size() - 1);
                mResults += values[0].getChildCount();
            }
            mBooksSearched++;
            mNumberOfAlreadySearchedBooksTextView.setText(String.valueOf(mBooksSearched));
            mProgressBar.setProgress(mBooksSearched);

        }

        @Override
        protected void onPostExecute(Void v) {
        }
    }
}
