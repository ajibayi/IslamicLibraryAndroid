package com.fekracomputers.islamiclibrary.search;

import android.content.Context;
import android.support.annotation.Nullable;

import com.fekracomputers.islamiclibrary.databases.BookDatabaseException;
import com.fekracomputers.islamiclibrary.databases.BookDatabaseHelper;
import com.fekracomputers.islamiclibrary.model.BookPartsInfo;
import com.fekracomputers.islamiclibrary.search.model.BookSearchResultsContainer;
import com.fekracomputers.islamiclibrary.search.model.SearchRequest;
import com.fekracomputers.islamiclibrary.search.model.SearchResult;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * بسم الله الرحمن الرحيم
 * Created by moda_ on 26/2/2017.
 */
public class BookSearcher {


    private final Context context;
    private final SearchRequest searchRequest;

    public BookSearcher(Context context, SearchRequest searchRequest) {
        this.context = context;
        this.searchRequest = searchRequest;
    }


    @Nullable
    public BookSearchResultsContainer getBookSearchResultsContainer(int bookId) {
        try {
            BookDatabaseHelper bookDatabaseHelper = BookDatabaseHelper.getInstance(context, bookId);
            ArrayList<SearchResult> results = bookDatabaseHelper.search(searchRequest);
            BookPartsInfo bookPartsInfo = bookDatabaseHelper.getBookPartsInfo();
            ListIterator<SearchResult> searchResultIterator = results.listIterator();
            while (searchResultIterator.hasNext()) {
                SearchResult searchResult = searchResultIterator.next();
                if (!searchResult.isRequired()) {
                    searchResultIterator.remove();
                }
            }
            return new BookSearchResultsContainer(searchRequest.isExpanded(), bookId, bookDatabaseHelper.getBookName(), bookPartsInfo, results);
        } catch (BookDatabaseException bookDatabaseException) {
            return new BookSearchResultsContainer(searchRequest.isExpanded(), bookId, "", null, new ArrayList<>());
        }
    }
}






