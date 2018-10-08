package com.fekracomputers.islamiclibrary.download.model;

/**
 * Created by Mohammad Yahia on 06/11/2016.
 */

public final class DownloadsConstants {

    public static final String BROADCAST_ACTION = "com.fekracomputers.islamiclibrary.download.BROADCAST";


    public static final String EXTRA_DOWNLOAD_STATUS = "com.fekracomputers.islamiclibrary.download.STATUS";
    public static final String EXTRA_DOWNLOAD_ID = "com.fekracomputers.islamiclibrary.download_id";
    public static final String EXTRA_DOWNLOAD_BOOK_ID = "bookId";
    public static final String EXTRA_NOTIFY_WITHOUT_BOOK_ID = "EXTRA_NOTIFY_WITHOUT_BOOK_ID";

    public static final int STATUS_INVALID = -3;
    public static final int STATUS_DOWNLOAD_CANCELLED=-2;
    public static final int STATUS_NOT_DOWNLOAD = -1;
    public static final int STATUS_DOWNLOAD_REQUESTED = 0;
    public static final int STATUS_DOWNLOAD_Pending = 1;
    public static final int STATUS_DOWNLOAD_STARTED = 2;
    public static final int STATUS_DOWNLOAD_COMPLETED = 3;
    public static final int STATUS_WAITING_FOR_UNZIP = 4;
    public static final int STATUS_UNZIP_STARTED = 5;
    public static final int STATUS_UNZIP_ENDED = 6;
    public static final int STATUS_FTS_INDEXING_STARTED = 7;
    public static final int STATUS_FTS_INDEXING_ENDED = 8;


    public static final int STATUS_INFORMATION_DATABASE_DOWNLOAD_ONLY_SUCCESSFUL = 103;
    public static final int STATUS_BOOKINFORMATION_WAITING_FOR_UNZIP = 104;
    public static final int STATUS_BOOKINFORMATION_UNZIP_STARTED = 105;
    public static final int STATUS_BOOKINFORMATION_UNZIP_ENDED = 106;
    public static final int STATUS_BOOKINFORMATION_FTS_INDEXING_STARTED = 107;
    public static final int STATUS_BOOKINFORMATION_FTS_INDEXING_ENDED = 108;


    public static final int BOOK_INFORMATION_DUMMY_ID = -10;

    public static final String EXTRA_DOWNLOAD_FAILLED_REASON = "com.fekracomputers.islamiclibrary.download.model.DownloadsConstants.EXTRA_DOWNLOAD_FAILLED_REASON";
    public static final int STATUS_BOOKINFORMATION_FAILED = 100;
    public static final String REASON_CANCELED_BY_USER = "canceled by user";
    public static final int CURRENT_BOOKS_INFO_CONTENT_VERSION = 1;
    public static final String PREF_BOOKS_INFO_CONTENT_VERSION = "pref_books_info_content_version";
}
