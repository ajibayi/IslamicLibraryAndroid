package com.fekracomputers.islamiclibrary.databases;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.fekracomputers.islamiclibrary.download.model.DownloadFileConstants;
import com.fekracomputers.islamiclibrary.download.reciver.BookDownloadCompletedReceiver;
import com.fekracomputers.islamiclibrary.model.AuthorInfo;
import com.fekracomputers.islamiclibrary.model.BookCategory;
import com.fekracomputers.islamiclibrary.model.BookInfo;
import com.fekracomputers.islamiclibrary.model.BookPartsInfo;
import com.fekracomputers.islamiclibrary.model.PageCitation;
import com.fekracomputers.islamiclibrary.model.PageInfo;
import com.fekracomputers.islamiclibrary.model.PartInfo;
import com.fekracomputers.islamiclibrary.model.Title;
import com.fekracomputers.islamiclibrary.search.model.SearchRequest;
import com.fekracomputers.islamiclibrary.search.model.SearchResult;
import com.fekracomputers.islamiclibrary.utility.ArabicUtilities;
import com.fekracomputers.islamiclibrary.utility.StorageUtils;
import com.fekracomputers.islamiclibrary.utility.SystemUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import timber.log.Timber;


public class BookDatabaseHelper extends SQLiteOpenHelper {


    public static final String IS_PARENT = "is_parent";

    public static final String POPULATE_BOOKS_FTS_SQL = "INSERT OR REPLACE INTO " + BookDatabaseContract.pagesTextSearch.TABLE_NAME +
            "(" +
            BookDatabaseContract.pagesTextSearch.COLUMN_NAME_DOC_id + SQL.COMMA +
            BookDatabaseContract.pagesTextSearch.COLUMN_NAME_PAGE +
            ")" +
            "VALUES (" + "?" + SQL.COMMA + " ?" + ")";
    public static final String POPULATE_TITLES_FTS_SQL = "INSERT OR REPLACE INTO " + BookDatabaseContract.titlesTextSearch.TABLE_NAME +
            "(" +
            BookDatabaseContract.titlesTextSearch.COLUMN_NAME_DOC_id + SQL.COMMA +
            BookDatabaseContract.titlesTextSearch.COLUMN_NAME_TITLE +
            ")" +
            "VALUES (" + "?" + SQL.COMMA + " ?" + ")";
    public static final int DATABASE_VERSION = 4;
    private static final String BOOK_FTS_QUERY_SQL = SQL.SELECT +
            BookDatabaseContract.searchResultPageTableAlias.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.pagesTextSearch.COLUMN_NAME_DOC_id + SQL.AS + BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PAGE_ID + SQL.COMMA +
            BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + SQL.AS + BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PARTNUMBER + SQL.COMMA +
            BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + SQL.AS + BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PAGENUMBER + SQL.COMMA +
            BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE + SQL.AS + BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PAGE + SQL.COMMA +
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID + SQL.AS + BookDatabaseContract.searchResultParentTitleTableAlias.PARENT_TITLE_ID + SQL.COMMA +
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE + SQL.AS + BookDatabaseContract.searchResultParentTitleTableAlias.PARENT_TITLE_TITLE + SQL.COMMA +
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.AS + BookDatabaseContract.searchResultParentTitleTableAlias.PARENT_TITLE_PAGE_ID +
            SQL.FROM +
            "(" + SQL.SELECT + BookDatabaseContract.pagesTextSearch.COLUMN_NAME_DOC_id + SQL.FROM + BookDatabaseContract.pagesTextSearch.TABLE_NAME + SQL.WHERE + BookDatabaseContract.pagesTextSearch.COLUMN_NAME_PAGE + SQL.MATCH + "?" + ")" +
            SQL.AS + BookDatabaseContract.searchResultPageTableAlias.TABLE_NAME +
            SQL.JOIN +
            BookDatabaseContract.TitlesEntry.TABLE_NAME +
            SQL.ON +
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.EQUALS + "(" + SQL.SELECT + " max(" + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + ")" + SQL.FROM + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.WHERE + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + "<=" + BookDatabaseContract.searchResultPageTableAlias.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.pagesTextSearch.COLUMN_NAME_DOC_id + ")" +
            SQL.JOIN +
            BookDatabaseContract.PageEntry.TABLE_NAME +
            SQL.ON +
            BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + SQL.EQUALS + BookDatabaseContract.searchResultPageTableAlias.TABLE_NAME + SQL.DOT_SEPARATOR + BookDatabaseContract.pagesTextSearch.COLUMN_NAME_DOC_id;
    private static final String TITLES_COLUMNS =
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID + SQL.COMMA +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE + SQL.COMMA +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID + SQL.COMMA +
                    BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + SQL.COMMA +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.COMMA +
                    BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + SQL.COMMA +
                    "CASE WHEN " + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID +
                    " IN (" + SQL.SELECT +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID
                    +
                    SQL.FROM +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    ") THEN 1  ELSE 0  END" + SQL.AS
                    + IS_PARENT;
    private static final String CREATE_TITLES_FTS_TABLE = "CREATE VIRTUAL TABLE IF NOT EXISTS " +
            BookDatabaseContract.titlesTextSearch.TABLE_NAME +
            " USING fts4(content=\"\"" + SQL.COMMA + " " + BookDatabaseContract.titlesTextSearch.COLUMN_NAME_TITLE + ")";
    private static final String OPTIMIZE_TITLES_FTS = " INSERT INTO " +
            BookDatabaseContract.titlesTextSearch.TABLE_NAME + "(" + BookDatabaseContract.titlesTextSearch.TABLE_NAME + ")" +
            "VALUES('optimize')";
    private static final String TAG = "BookDatabaseHelper";
    private static final String SELECT_TITLES = SQL.SELECT +
            TITLES_COLUMNS +
            " from " +
            BookDatabaseContract.TitlesEntry.TABLE_NAME +
            SQL.JOIN +
            BookDatabaseContract.PageEntry.TABLE_NAME +
            SQL.ON +
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.EQUALS +
            BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID;
    private static final String TITLES_FTS_QUERY_SQL = SELECT_TITLES +
            SQL.WHERE +
            BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID +
            " In (" +
            SQL.SELECT +
            BookDatabaseContract.titlesTextSearch.TABLE_NAME + SQL.DOT + BookDatabaseContract.titlesTextSearch.COLUMN_NAME_DOC_id +
            SQL.FROM +
            BookDatabaseContract.titlesTextSearch.TABLE_NAME +
            " WHERE " +
            BookDatabaseContract.titlesTextSearch.TABLE_NAME + SQL.DOT + BookDatabaseContract.titlesTextSearch.COLUMN_NAME_TITLE +
            SQL.MATCH +
            "?" + ")" +
            " Order By " + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID;
    private static final String GET_TITLES_UNDER_PARENT_QUERY =
            SELECT_TITLES +
                    " where " +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID + "=?";
    private static final String GET_PARENT_TREE_QUERY =
            "WITH RECURSIVE" +
                    " parent_of(id" + SQL.COMMA + " parentid) AS " +
                    " (SELECT " + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID + SQL.COMMA +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID +
                    SQL.FROM + BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    " )" + SQL.COMMA +
                    " ancestor_of(id) AS " +
                    "(SELECT parentid" + SQL.FROM + "parent_of WHERE id=? " +
                    " UNION ALL " +
                    SQL.SELECT + "parentid" + SQL.FROM + "parent_of JOIN ancestor_of USING(id)) " +
                    SQL.SELECT +
                    TITLES_COLUMNS +
                    SQL.FROM + "ancestor_of join " + BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    " on ancestor_of" + SQL.DOT_SEPARATOR + "id=" +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID +
                    SQL.JOIN +
                    BookDatabaseContract.PageEntry.TABLE_NAME +
                    SQL.ON +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.EQUALS +
                    BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID;
    /**
     * this query is vey fast although very large :)
     */
    private static final String GET_PAGE_PARENT_TREE_QUERY =
            "WITH RECURSIVE parent_of(id" + SQL.COMMA + " parentid)\n" +
                    "    AS  (SELECT " + BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID + SQL.COMMA +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID +
                    SQL.FROM + BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    " )" + SQL.COMMA +
                    " \n" +
                    "    ancestor_of(id) AS\n" +
                    "     (SELECT parentid\n" +
                    "       " + SQL.FROM + "parent_of \n" +
                    "        WHERE id=(\n" +
                    "        " +
                    SQL.SELECT +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID +
                    " from " +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    SQL.JOIN +
                    BookDatabaseContract.PageEntry.TABLE_NAME +
                    SQL.ON +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.EQUALS +
                    BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID +
                    " where " +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID +
                    " <= ? order by " + BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " desc  limit 1 ) \n" +
                    "            UNION ALL \n" +
                    "            " +
                    SQL.SELECT + "parentid" + SQL.FROM + "parent_of JOIN ancestor_of USING(id)) \n" +
                    "" +
                    SQL.SELECT +
                    TITLES_COLUMNS +
                    SQL.FROM + "ancestor_of \n" +
                    " join " + BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    " \n" +
                    " on ancestor_of" + SQL.DOT_SEPARATOR + "id=" +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID +
                    SQL.JOIN +
                    BookDatabaseContract.PageEntry.TABLE_NAME +
                    SQL.ON +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + SQL.EQUALS +
                    BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID;
    private static final String CREATE_BOOK_FTS_TABLE = "CREATE VIRTUAL TABLE IF NOT EXISTS " +
            BookDatabaseContract.pagesTextSearch.TABLE_NAME +
            " USING fts4(content=\"\"" + SQL.COMMA + " page)";
    private static final String OPTIMIZE_BOOK_FTS = " INSERT INTO " +
            BookDatabaseContract.pagesTextSearch.TABLE_NAME + "(" + BookDatabaseContract.pagesTextSearch.TABLE_NAME + ")" +
            "VALUES('optimize')";
    //" USING fts4(page TEXT)";
    @NonNull
    private static SparseArray<BookDatabaseHelper> sIsnstances = new SparseArray<>();
    private final int bookId;
    //  private SQLiteDatabase mDatabase = null;

    /**
     * the full path to the database including file extension
     */
    private String mBookPath;

    private BookDatabaseHelper(@NonNull Context context, int mBookId) {
        //super(new DatabaseContext(context),mBookId+".sqlite", null, 1);
        super(context,
                StorageUtils.getIslamicLibraryShamelaBooksDir(context) +
                        File.separator + mBookId + SQL.DOT_SEPARATOR + DownloadFileConstants.DATABASE_FILE_EXTENSTION,
                null,
                DATABASE_VERSION);
        this.bookId = mBookId;
        mBookPath = StorageUtils.getIslamicLibraryShamelaBooksDir(context) + File.separator + Integer.toString(mBookId) + SQL.DOT_SEPARATOR + BooksInformationDbHelper.DATABASE_EXTENSION;

    }


    /**
     * @param context
     * @param bookId
     * @return
     * @throws BookDatabaseException if the file is corrupted also delete book entry from book information database
     */
    public static synchronized BookDatabaseHelper getInstance(@NonNull Context context, int bookId) throws BookDatabaseException {
        if (sIsnstances.indexOfKey(bookId) < 0) { //no instance for this book already exists
            sIsnstances.append(bookId, new BookDatabaseHelper(context, bookId));
        }
        BookDatabaseHelper sqLiteOpenHelper = sIsnstances.get(bookId);
        if (sqLiteOpenHelper.isValidBook()) {
            return sqLiteOpenHelper;
        } else {
            DBValidator dBValidator = new DBValidator(DBValidator.DataBaseType.BOOK_DATABASE_TYPE);
            dBValidator.validate(sqLiteOpenHelper);
            deleteInvalidBook(bookId, context);
            throw new BookDatabaseException(dBValidator.getCause(), bookId, sqLiteOpenHelper.mBookPath);
        }
    }

    private static synchronized BookDatabaseHelper getInstanceNoExeption(@NonNull Context context, int bookId) {
        if (sIsnstances.indexOfKey(bookId) < 0) { //no instance for this book already exists
            sIsnstances.append(bookId, new BookDatabaseHelper(context, bookId));
        }
        return sIsnstances.get(bookId);

    }

    public static void deleteInvalidBook(int bookId, @NonNull Context context) {
        BooksInformationDbHelper.getInstance(context).deleteBook(bookId, context);
        BookDownloadCompletedReceiver.broadCastBookDownloadFailed(bookId, "invalidDatabase", context);
    }

    static synchronized void closeStatic(int bookId, Context context) {
        getInstanceNoExeption(context, bookId).close();
    }

    public static boolean isValidBookStatic(int bookId, Context conext) {
        DBValidator dBValidator = new DBValidator(DBValidator.DataBaseType.BOOK_DATABASE_TYPE);
        dBValidator.validate(getInstanceNoExeption(conext, bookId));
        return dBValidator.isValid();
    }

    @Override
    public synchronized void close() {
        super.close();
        //TODO needs more investigation it seems the safest thing is not to close the helper
        sIsnstances.delete(bookId);//delete the instance
    }

    private Title cursorToTitle(@NonNull Cursor c, int coulmn_title_id_index, int column_title_text_indexd, int column_pageId_indexd) {

        return new Title(c.getInt(coulmn_title_id_index),
                0,
                0,
                0,
                c.getInt(column_pageId_indexd),
                c.getString(column_title_text_indexd),
                false
        );
    }

    /**
     * @param cursor already move cursor
     * @return Title object corresponding to this
     */
    @NonNull
    public Title cursorToTitle(@NonNull Cursor cursor,
                               int coulmn_id_index,
                               int coulmn_parentid_index,
                               int column_partnumber_indexd,
                               int column_title_text_indexd,
                               int column_rowId_indexd,
                               int column_original_page_indexd,
                               int coulmn_is_parent_index
    ) {


        return new Title(cursor.getInt(coulmn_id_index),
                cursor.getInt(coulmn_parentid_index),
                cursor.getInt(column_original_page_indexd),
                cursor.getInt(column_partnumber_indexd),
                cursor.getInt(column_rowId_indexd),
                cursor.getString(column_title_text_indexd),
                cursor.getInt(coulmn_is_parent_index) == 1
        );
    }

    /**
     * @param pageId the page to find its direct parent
     */
    public Title getParentTitle(int pageId) {
        return getParentTitle(pageId, true);
    }

    /**
     * @param pageId    the page to find its direct parent
     * @param inclusive true to conider the page as parent title for itself if possible
     */
    public Title getParentTitle(int pageId, boolean inclusive) {

        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TITLES +
                " where " +
                BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID +
                (inclusive ? " <= ?" : "<?") +
                " order by " + BookDatabaseContract.PageEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " desc "
                + " limit 1 ", new String[]{String.valueOf(pageId)});
        int coulmn_id_index = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID);
        int coulmn_parentid_index = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID);
        int column_partnumber_indexd = cursor.getColumnIndex(BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER);
        int column_title_text_indexd = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE);
        int column_rowId_indexd = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID);
        int column_original_page_indexd = cursor.getColumnIndex(BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER);
        int coulmn_is_parent_index = cursor.getColumnIndex(BookDatabaseHelper.IS_PARENT);
        Title title;
        if (cursor.moveToFirst()) {
            title = cursorToTitle(
                    cursor,
                    coulmn_id_index,
                    coulmn_parentid_index,
                    column_partnumber_indexd,
                    column_title_text_indexd,
                    column_rowId_indexd,
                    column_original_page_indexd,
                    coulmn_is_parent_index
            );

        } else {

            return Title.createRootTitle(getBookName());
        }
        cursor.close();
        return title;
    }

    public String getBookName() {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.InfoEntry.TABLE_NAME,
                new String[]{BookDatabaseContract.InfoEntry.COLUMN_NAME_VALUE}
                , BookDatabaseContract.InfoEntry.COLUMN_NAME_NAME + "='" + BookDatabaseContract.InfoEntry.KEY_BOOK_TITLE + "'",
                null,
                null,
                null,
                null);
        c.moveToFirst();
        String bookTitle = c.getString(0);
        c.close();
        return bookTitle;
    }

    public Cursor getTitles() {

        return getReadableDatabase().query(BookDatabaseContract.TitlesEntry.TABLE_NAME,
                new String[]{
                        BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID,
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER,
                        BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID,
                        BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID,
                        BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE,

                }, null, null, null, null, BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID
        );

    }

    /**
     * @param parentId the parent id to list titles under it
     * @return a cursor representing titles the cursor fields are from
     * <p></p>
     * {@link BookDatabaseContract.TitlesEntry#COLUMN_NAME_ID}
     * </p><p>
     * {@link BookDatabaseContract.TitlesEntry#COLUMN_NAME_TITLE}
     * * </p><p>
     * {@link BookDatabaseContract.TitlesEntry#COLUMN_NAME_PARENT_ID}
     * </p><p>
     * {@link BookDatabaseContract.TitlesEntry#COLUMN_NAME_PAGE_ID}
     * </p><p>
     * {@link BookDatabaseContract.PageEntry#COLUMN_NAME_PAGE_ID}
     * * </p><p>
     * {@link BookDatabaseContract.PageEntry#COLUMN_NAME_PART_NUMBER }
     * </p><p>
     * {@link BookDatabaseContract.PageEntry#COLUMN_NAME_PAGE_NUMBER}
     * </p>
     */
    public Cursor getTitlesUnder(int parentId) {
        return getReadableDatabase().rawQuery(GET_TITLES_UNDER_PARENT_QUERY, new String[]{String.valueOf(parentId)});
    }

    /**
     * @param titleId  the title id to return its positin
     * @param parentId
     * @return the o based position of this title within is parent
     */
    public int getTitlePositionUnderParent(int titleId, int parentId) {
        return (int) DatabaseUtils.longForQuery(getReadableDatabase(),
                SQL.SELECT + "count(*)" + SQL.FROM +
                        "(" + SQL.SELECT + " null " + SQL.FROM + BookDatabaseContract.TitlesEntry.TABLE_NAME +
                        SQL.WHERE +
                        BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID + "=? and " +
                        BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID + "<? )",
                new String[]{String.valueOf(parentId), String.valueOf(titleId)});
    }

    @NonNull
    public LinkedList<Title> buildTableOfContentHistoryToTitle(int titleId) {

        LinkedList<Title> titlesHistory = new LinkedList<>();

        if (SystemUtils.runningOnLollipopOrLater()) {
            Cursor cursor = getReadableDatabase().rawQuery(GET_PARENT_TREE_QUERY, new String[]{String.valueOf(titleId)});

            int coulmn_id_index = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID);
            int coulmn_parentid_index = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID);
            int column_partnumber_indexd = cursor.getColumnIndex(BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER);
            int column_title_text_indexd = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE);
            int column_rowId_indexd = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID);
            int column_original_page_indexd = cursor.getColumnIndex(BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER);
            int coulmn_is_parent_index = cursor.getColumnIndex(BookDatabaseHelper.IS_PARENT);


            while (cursor.moveToNext()) {
                Title title;
                title = cursorToTitle(
                        cursor,
                        coulmn_id_index,
                        coulmn_parentid_index,
                        column_partnumber_indexd,
                        column_title_text_indexd,
                        column_rowId_indexd,
                        column_original_page_indexd,
                        coulmn_is_parent_index
                );
                titlesHistory.addFirst(title);
            }
            titlesHistory.addFirst(Title.createRootTitle(getBookName()));
            cursor.close();
        } else {
            //Recursive CTE no available
            Title title;
            if (titleId != 0) {
                title = getTitleById(titleId);
                while (title.parentId != 0) {
                    title = getTitleById(title.parentId);
                    titlesHistory.addFirst(title);
                }
            }
            titlesHistory.addFirst(Title.createRootTitle(getBookName()));


        }
        return titlesHistory;
    }

    private Title getTitleById(int titleId) {

        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TITLES +
                        " where " +
                        BookDatabaseContract.TitlesEntry.TABLE_NAME + SQL.DOT + BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID +
                        "=?"
                , new String[]{String.valueOf(titleId)});
        int coulmn_id_index = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID);
        int coulmn_parentid_index = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID);
        int column_partnumber_indexd = cursor.getColumnIndex(BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER);
        int column_title_text_indexd = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE);
        int column_rowId_indexd = cursor.getColumnIndex(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID);
        int column_original_page_indexd = cursor.getColumnIndex(BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER);
        int coulmn_is_parent_index = cursor.getColumnIndex(BookDatabaseHelper.IS_PARENT);
        Title title;
        if (cursor.moveToFirst()) {
            title = cursorToTitle(
                    cursor,
                    coulmn_id_index,
                    coulmn_parentid_index,
                    column_partnumber_indexd,
                    column_title_text_indexd,
                    column_rowId_indexd,
                    column_original_page_indexd,
                    coulmn_is_parent_index
            );

        } else {

            return Title.createRootTitle(getBookName());
        }
        cursor.close();
        return title;
    }

    public int getPageCount() {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME,
                new String[]{"count(*)"},
                null, null, null, null, null
        );
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        return count;
    }

    public String getPageContentByOriginalPageNumber(int partNumber, int pageNumber) {

        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE},
                BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + " = ? and " +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + " = ?",
                new String[]{String.valueOf(partNumber), String.valueOf(pageNumber)},
                null, null, null
        );
        c.moveToFirst();
        String page_content = c.getString(0);
        c.close();
        return page_content;
    }

    public String getPageContentByPageId(int pageId) {

        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE},
                BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " = ?",
                new String[]{String.valueOf(pageId)},
                null, null, null
        );
        try {
            c.moveToFirst();
            String page_content = c.getString(0);
            c.close();
            return page_content;
        } catch (Exception e) {
            Timber.e("getPageContentByPageId(" + pageId + ")" + "from book" + mBookPath, e);
        } finally {
            c.close();
        }

        return "Error getPageContentByPageId(" + pageId + ")" + "from book" + mBookPath;

    }

    @NonNull
    public PageInfo getPageInfoByPageId(int pageId) {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{

                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER,
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER
                },
                BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " = ?",
                new String[]{String.valueOf(pageId)},
                null, null, null
        );
        c.moveToFirst();

        int originalPageNumber = c.getInt(0);
        int partNumber = c.getInt(1);
        c.close();


        return new PageInfo(pageId, partNumber, originalPageNumber);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        onUpgrade(db, db.getVersion(), DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            try {
                db.execSQL("alter table " + BookDatabaseContract.pagesTextSearch.TABLE_NAME_V3 +
                        " rename to " + BookDatabaseContract.pagesTextSearch.TABLE_NAME);

            } catch (SQLException e) {
                Timber.d(e);
            }
            //CREATE INDEX `titles_desc` ON `titles` (`pageid` DESC)
            db.execSQL(SQL.CREATE_INDEX_IF_NOT_EXISTS +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    "titles_PageId_index" +
                    SQL.ON +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    "(" + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID + " DESC " + ")");

            //CREATE INDEX `titles_parent` ON `titles` (`parentid` )
            db.execSQL(SQL.CREATE_INDEX_IF_NOT_EXISTS +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    "titles_parentId_index" +
                    SQL.ON +
                    BookDatabaseContract.TitlesEntry.TABLE_NAME +
                    "(" + BookDatabaseContract.TitlesEntry.COLUMN_NAME_PARENT_ID + ")");

            //CREATE  INDEX `partNumberPageNumberIndex` ON `pages` (`partnumber` ,`pagenumber` );
            db.execSQL(SQL.CREATE_INDEX_IF_NOT_EXISTS +
                    BookDatabaseContract.PageEntry.TABLE_NAME +
                    "partNumberPageNumberIndex" +
                    SQL.ON +
                    BookDatabaseContract.PageEntry.TABLE_NAME +
                    "(" + BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + "," + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + ")");

        }
    }

    @NonNull
    public PageCitation getCitationInformation(int pageRowId) {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.InfoEntry.TABLE_NAME,
                new String[]{BookDatabaseContract.InfoEntry.COLUMN_NAME_VALUE
                }
                , BookDatabaseContract.InfoEntry.COLUMN_NAME_NAME + "='" + BookDatabaseContract.InfoEntry.KEY_BOOK_TITLE + "'" +
                        " OR " +
                        BookDatabaseContract.InfoEntry.COLUMN_NAME_NAME + "='" + BookDatabaseContract.InfoEntry.KEY_AUTHOUR_NAME + "'"
                ,
                null,
                null,
                null,
                null);
        c.moveToFirst();
        String bookTitle = c.getString(0);
        c.moveToNext();
        String authorName = c.getString(0);
        c.close();

        return new PageCitation(bookTitle, authorName, getPageInfoByPageId(pageRowId), getBookPartsInfo());
    }

    public int getPageId(int partNumber, int pageNumber) {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID},
                BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + " = ? and " +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + " = ?",
                new String[]{String.valueOf(partNumber), String.valueOf(pageNumber)},
                null, null, null
        );
        c.moveToFirst();
        int pageId = 1;
        try {
            pageId = c.getInt(0);
        } catch (SQLException e) {
            Timber.e("getPageId: ", e);
        } finally {
            c.close();
        }
        return pageId;
    }

    @NonNull
    public PartInfo getPartInfo(int partNumber) {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{"min(" + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + ")" + SQL.COMMA + "max(" +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + ")"
                },
                BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + " = ? ",
                new String[]{String.valueOf(partNumber)},
                null, null, null
        );
        c.moveToFirst();
        PartInfo partInfo = new PartInfo(c.getInt(0), c.getInt(1), partNumber);
        c.close();
        return partInfo;

    }

    @NonNull
    public BookPartsInfo getBookPartsInfo() {
        Cursor c = getReadableDatabase().rawQuery("select real_minimum+part_offset as min_part ,max_part,max_page  \n" +
                        "from (\n" +
                        " select min(pages.partnumber) as real_minimum,\n" +
                        " max(" + BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + ") as max_part,\n" +
                        " max(" + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + ") as max_page,\n" +
                        " case (select count(" + BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + ") from " +
                        BookDatabaseContract.PageEntry.TABLE_NAME + " where " +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + "=0 and " +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + "=0)  \n" +
                        " when 1 \n" +
                        " then  1  \n" +
                        " else 0  \n" +
                        " end \n" +
                        " as part_offset  \n" +
                        " from " + BookDatabaseContract.PageEntry.TABLE_NAME +
                        " )",
                null
        );
        c.moveToFirst();

        BookPartsInfo bookPartsInfo = new BookPartsInfo(getPartInfo(c.getInt(0)), c.getInt(1), c.getInt(2));
        c.close();
        return bookPartsInfo;

    }

    @NonNull
    public PageInfo getFirstPageInfo() {

        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID,
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER,
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER
                },
                null,
                null,
                null,
                null,
                BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " ASC ",
                "1"
        );
        c.moveToFirst();
        int pageId = c.getInt(0);
        int originalPageNumber = c.getInt(1);
        int partNumber = c.getInt(2);
        c.close();


        return new PageInfo(pageId, partNumber, originalPageNumber);
    }

    public int position2PageId(int position) {

        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID
                },
                null,
                null,
                null,
                null,
                BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " ASC ",
                position + SQL.COMMA + 1
        );
        c.moveToFirst();
        int pageId = c.getInt(0);
        c.close();

        return pageId;
    }

    public int pageId2position(int pageId) {


        Cursor c = getReadableDatabase().query(BookDatabaseContract.PageEntry.TABLE_NAME
                , new String[]{
                        "count(*)-1"
                },
                BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " <=?",
                new String[]{String.valueOf(pageId)},
                null,
                null,
                null,
                null
        );
        c.moveToFirst();
        int position = c.getInt(0);
        c.close();
        if (position < 0) throw new IndexOutOfBoundsException("this page id doesn't exisit");
        return position;
    }

    @NonNull
    public PageInfo getPageInfoByPagePageNumberAndPartNumber(int partNumber, int pageNumber) {
        return new PageInfo(getPageId(partNumber, pageNumber), partNumber, pageNumber);
    }

    @NonNull
    public PageInfo getPageInfoByPagePosition(int position) {
        Cursor c = getReadableDatabase().query(
                BookDatabaseContract.PageEntry.TABLE_NAME,
                new String[]{
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID,
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER,
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER
                },
                null,
                null,
                null,
                null,
                BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID + " ASC ",
                position + SQL.COMMA + 1
        );
        c.moveToFirst();
        int pageId = c.getInt(0);
        int pageNumber = c.getInt(1);
        int partNumber = c.getInt(2);
        c.close();


        return new PageInfo(pageId, partNumber, pageNumber);
    }

    public Cursor searchTitles(String searchString) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(TITLES_FTS_QUERY_SQL
                , new String[]{searchString});

        return c;
    }

    @NonNull
    public ArrayList<SearchResult> search(SearchRequest searchRequest) {
        String cleanedSearchString = searchRequest.getCleanedSearchString();

        /*
        select pages.id,pages.partnumber,pages.pagenumber,pages.page
        from pages
        where pages.id in (select docid from pagesTextSearch where pagesTextSearch.page match "نَافِع")
*/
        ArrayList<SearchResult> SearchResults = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(BOOK_FTS_QUERY_SQL
                , new String[]{cleanedSearchString});

        final int COLUMN_NAME_PAGE_ID_IDX = c.getColumnIndex(BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PAGE_ID);
        final int COLUMN_NAME_PART_NUMBER_IDX = c.getColumnIndex(BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PARTNUMBER);
        final int COLUMN_NAME_PAGE_NUMBER_IDX = c.getColumnIndex(BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PAGENUMBER);
        final int COLUMN_NAME_PAGE_IDX = c.getColumnIndex(BookDatabaseContract.searchResultPageTableAlias.SEARCH_RESULT_PAGE);

        int coulmn_title_id_index = c.getColumnIndex(BookDatabaseContract.searchResultParentTitleTableAlias.PARENT_TITLE_ID);
        int column_title_text_indexd = c.getColumnIndex(BookDatabaseContract.searchResultParentTitleTableAlias.PARENT_TITLE_TITLE);
        int column_rowId_indexd = c.getColumnIndex(BookDatabaseContract.searchResultParentTitleTableAlias.PARENT_TITLE_PAGE_ID);


        while (c.moveToNext()) {
            Title title = cursorToTitle(
                    c,
                    coulmn_title_id_index,
                    column_title_text_indexd,
                    column_rowId_indexd
            );
            SearchResult SearchResult = new SearchResult(bookId, c.getInt(COLUMN_NAME_PAGE_ID_IDX),
                    c.getInt(COLUMN_NAME_PART_NUMBER_IDX),
                    c.getInt(COLUMN_NAME_PAGE_NUMBER_IDX),
                    c.getString(COLUMN_NAME_PAGE_IDX)
                    , searchRequest.searchOptions,
                    searchRequest.searchString,
                    title);

            SearchResults.add(SearchResult);
        }
        c.close();
        return SearchResults;
    }

    public boolean indexFts() throws SQLException {
        if (isFtsSearchable()) return true;
        SQLiteDatabase db = getWritableDatabase();

        Cursor allPagesCursor = null;
        Cursor allTitlesCursor = null;
        db.beginTransaction();
        try {
            allPagesCursor = db.query(BookDatabaseContract.PageEntry.TABLE_NAME,
                    new String[]{BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_ID,
                            BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE},
                    null,
                    null,
                    null,
                    null,
                    null
            );
            db.execSQL(CREATE_BOOK_FTS_TABLE);
            SQLiteStatement populateFTS_Statement = db.compileStatement(POPULATE_BOOKS_FTS_SQL); //pre-compiled sql statement
            while (allPagesCursor.moveToNext()) {
                String cleanedText = ArabicUtilities.cleanTextForSearchingIndexing(allPagesCursor.getString(1));
                populateFTS_Statement.clearBindings();
                populateFTS_Statement.bindLong(1, allPagesCursor.getLong(0));
                populateFTS_Statement.bindString(2, cleanedText);
                populateFTS_Statement.executeInsert();
            }
            db.rawQuery(OPTIMIZE_BOOK_FTS, null);


            allTitlesCursor = db.query(BookDatabaseContract.TitlesEntry.TABLE_NAME,
                    new String[]{BookDatabaseContract.TitlesEntry.COLUMN_NAME_ID,
                            BookDatabaseContract.TitlesEntry.COLUMN_NAME_TITLE},
                    null,
                    null,
                    null,
                    null,
                    null
            );
            db.execSQL(CREATE_TITLES_FTS_TABLE);
            SQLiteStatement populateTitlesFTS_Statement = db.compileStatement(POPULATE_TITLES_FTS_SQL); //pre-compiled sql statement
            while (allTitlesCursor.moveToNext()) {
                String cleanedText = ArabicUtilities.cleanTextForSearchingIndexing(allTitlesCursor.getString(1));
                populateTitlesFTS_Statement.clearBindings();
                populateTitlesFTS_Statement.bindLong(1, allTitlesCursor.getLong(0));
                populateTitlesFTS_Statement.bindString(2, cleanedText);
                populateTitlesFTS_Statement.executeInsert();
            }
            db.rawQuery(OPTIMIZE_TITLES_FTS, null);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
            if (allPagesCursor != null) {
                allPagesCursor.close();
            }
            if (allTitlesCursor != null) {
                allTitlesCursor.close();
            }

        }


    }

    public boolean isFtsSearchable() {
        Cursor c = null;
        boolean b1;
        try {
            c = getReadableDatabase().rawQuery(
                    "select docid from " + BookDatabaseContract.pagesTextSearch.TABLE_NAME
                            + " where " + BookDatabaseContract.pagesTextSearch.COLUMN_NAME_PAGE + " match ? limit 1",
                    new String[]{"الله"});

            b1 = c.getCount() > -1;
        } catch (Exception e) {
            b1 = false;
        }
        if (c != null) {
            c.close();
        }
        boolean titlesSearch;
        Cursor titlesSearchCursor = null;
        try {
            titlesSearchCursor = getReadableDatabase().rawQuery(
                    "select docid from " + BookDatabaseContract.titlesTextSearch.TABLE_NAME
                            + " where " + BookDatabaseContract.titlesTextSearch.COLUMN_NAME_TITLE + " match ?  limit 1 ",
                    new String[]{"الله"});

            titlesSearch = titlesSearchCursor.getCount() > -1;
        } catch (Exception e) {
            titlesSearch = false;
        }
        if (titlesSearchCursor != null) {
            titlesSearchCursor.close();
        }
        close();
        return b1 && titlesSearch;

    }

    public boolean isPartPageCombinationValid(int partNumber, int pageNumber) {
        return DatabaseUtils.longForQuery(getReadableDatabase(),
                SQL.SELECT + " count(*) " + SQL.FROM +
                        "(" + SQL.SELECT + SQL.NULL + SQL.FROM + BookDatabaseContract.PageEntry.TABLE_NAME
                        + SQL.WHERE +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PART_NUMBER + "=?" + SQL.AND +
                        BookDatabaseContract.PageEntry.COLUMN_NAME_PAGE_NUMBER + "=?)",
                new String[]{String.valueOf(partNumber), String.valueOf(pageNumber)}) > 0L;
    }

    public boolean isValidBook() {
        DBValidator dBValidator = new DBValidator(DBValidator.DataBaseType.BOOK_DATABASE_TYPE);
        dBValidator.validate(this);
        return dBValidator.isValid();
    }

    @NonNull
    public BookInfo getBookInfo() {
        Cursor c = getReadableDatabase().query(BookDatabaseContract.InfoEntry.TABLE_NAME,
                new String[]{BookDatabaseContract.InfoEntry.COLUMN_NAME_NAME, BookDatabaseContract.InfoEntry.COLUMN_NAME_VALUE}
                , null, null, null, null, null
        );
        HashMap<String, String> map = new HashMap<>();
        while (c.moveToNext()) {
            map.put(c.getString(0), c.getString(1));
        }
        c.close();
        return new BookInfo(bookId,
                map.get(BookDatabaseContract.InfoEntry.KEY_BOOK_TITLE),
                map.get(BookDatabaseContract.InfoEntry.KEY_BOOK_INFORMATION),
                map.get(BookDatabaseContract.InfoEntry.KEY_BOOK_CARD),
                new AuthorInfo(
                        Integer.valueOf(map.get(BookDatabaseContract.InfoEntry.KEY_AUTHOR_ID)),
                        map.get(BookDatabaseContract.InfoEntry.KEY_AUTHOUR_NAME),
                        map.get(BookDatabaseContract.InfoEntry.KEY_AUTHOR_INFORMATION),
                        Integer.valueOf(map.get(BookDatabaseContract.InfoEntry.KEY_AUTHOR_DEATH_HIGRI_YEAR))
                ),
                new BookCategory(
                        Integer.valueOf(map.get(BookDatabaseContract.InfoEntry.KEY_CATEGORY_ID)),
                        map.get(BookDatabaseContract.InfoEntry.KEY_CATEGORY_TITLE))
        );
    }
}
