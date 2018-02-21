package com.fekracomputers.islamiclibrary.model;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;

import com.fekracomputers.islamiclibrary.R;
import com.fekracomputers.islamiclibrary.databases.UserDataDBHelper;

/**
 * Created by Mohammad on 23/10/2017.
 */

public class BooksCollection implements Comparable<BooksCollection> {

    private int order;
    private boolean visibility;
    private int automaticID;
    private String name;
    private Cursor cursor;
    private int booksCollectionId;

    public BooksCollection(int order, boolean visibility, int automaticID, String name, int booksCollectionId) {
        this.order = order;
        this.visibility = visibility;
        this.automaticID = automaticID;
        this.name = name;
        this.booksCollectionId = booksCollectionId;
    }

    public static BooksCollection fakeCollection(int collectionId) {
        return new BooksCollection(0, true, 0, "", collectionId);
    }

    @DrawableRes
    public static int getActionResId(int id) {
        switch (id) {
            case R.id.menu_item_clear:
                return R.drawable.ic_clear_all_black_24dp;
            case R.id.menu_delete_collection:
                return R.drawable.ic_delete_black_24dp;
            case R.id.menu_item_rename:
                return R.drawable.ic_edit_black_24dp;
            case R.id.menu_move_up:
                return R.drawable.ic_arrow_upward_black_24dp;
            case R.id.menu_move_down:
                return R.drawable.ic_arrow_downward_black_24dp;
            default:
                return 0;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Cursor getNewCursor(Context context) {
        if (cursor == null || cursor.isClosed()) {
            cursor = UserDataDBHelper.getInstance(context).getBooksCollectionCursor(this, context);
        }
        return cursor;
    }

    public Cursor reAcquireCursor(Context context, boolean refresh) {

        if (refresh) {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            return getNewCursor(context);
        }
        if (cursor != null && !cursor.isClosed()) {
            return cursor;
        } else {
            return getNewCursor(context);
        }
    }

    @Override
    public int hashCode() {
        return booksCollectionId;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BooksCollection) && (booksCollectionId == ((BooksCollection) obj).booksCollectionId);
    }

    @Override
    public int compareTo(@NonNull BooksCollection o) {
        //if you cant understand this then press alt-enter and convert ?: ti if else recursivly until it is full expanded :)
        return order != o.order ?
                order < o.order ?
                        -1 :
                        1 :
                booksCollectionId < o.booksCollectionId ? -1 :
                        booksCollectionId == o.booksCollectionId ? 0 :
                                1;
    }

    public boolean isVisibile() {
        return visibility;
    }

    public boolean isAutomatic() {
        return automaticID != 0;
    }

    public int getCollectionsId() {
        return booksCollectionId;
    }

    public int getAutomaticId() {
        return automaticID;
    }

    @MenuRes
    public int getMoreMenuRes() {
        if (isAutomatic()) {
            return R.menu.automatic_collection;
        } else {
            if (!isFavourie()) {
                return R.menu.user_collection;
            } else {
                return R.menu.favourite_collection;
            }
        }
    }

    private boolean isFavourie() {
        return booksCollectionId == UserDataDBHelper.GlobalUserDBHelper.FAVOURITE_COLLECTION_ID;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActionSupported(int id) {
        switch (id) {
            case R.id.menu_item_clear:
                return !isAutomatic();
            case R.id.menu_delete_collection:
                return !isAutomatic()&&!isFavourie();
            case R.id.menu_item_rename:
                return !isAutomatic()&&!isFavourie();
            case R.id.menu_move_up:
                return true;
            case R.id.menu_move_down:
                return true;
            default:
                return false;
        }
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }
}
