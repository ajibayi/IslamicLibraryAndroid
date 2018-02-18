package com.fekracomputers.islamiclibrary.reading.fragments;


import android.animation.Animator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.fekracomputers.islamiclibrary.R;
import com.fekracomputers.islamiclibrary.databases.BookDatabaseContract;
import com.fekracomputers.islamiclibrary.databases.BookDatabaseException;
import com.fekracomputers.islamiclibrary.databases.BookDatabaseHelper;
import com.fekracomputers.islamiclibrary.databases.BooksInformationDBContract;
import com.fekracomputers.islamiclibrary.databases.UserDataDBHelper;
import com.fekracomputers.islamiclibrary.model.BookInfo;
import com.fekracomputers.islamiclibrary.model.Highlight;
import com.fekracomputers.islamiclibrary.model.PageCitation;
import com.fekracomputers.islamiclibrary.model.PageInfo;
import com.fekracomputers.islamiclibrary.reading.ActionModeChangeListener;
import com.fekracomputers.islamiclibrary.reading.ReadingActivity;
import com.fekracomputers.islamiclibrary.reading.dialogs.DisplayPrefChangeListener;
import com.fekracomputers.islamiclibrary.reading.dialogs.NotePopupFragment;
import com.fekracomputers.islamiclibrary.utility.AppConstants;
import com.fekracomputers.islamiclibrary.utility.ArabicUtilities;
import com.fekracomputers.islamiclibrary.widget.AnimationUtils;

import java.util.Formatter;
import java.util.Locale;

import timber.log.Timber;

import static com.fekracomputers.islamiclibrary.R.id.highlight_remove;


public class BookPageFragment extends Fragment implements
        DisplayPrefChangeListener,
        ActionModeChangeListener,
        NotePopupFragment.HighlightNoteDialogListener {

    private static final String ANDROID_ASSET = "file:///android_asset/";
    private static final String KEY_PAGER_POSITION = "PAGER_POSITION";
    /**
     * 0 didn't start yet
     * 1 in action mode
     * 2 dismissed
     */
    private static final int ACTION_MODE_NOT_STARTED = 0;
    private static final int ACTION_MODE_STARTED = 1;
    private static final int ACTION_MODE_DISMISSED = 2;
    private static final String KEY_TASHKEEL_ON = "tashkeelOn";
    private final float SCROLL_THRESHOLD = 10;
    public String page_content;
    UserDataDBHelper userDataDBHelper;
    int pageId;
    @Nullable
    private PageFragmentListener pageFragmentListener;
    private int bookId;
    private WebView mBookPageWebView;
    private int mIsInActionMode = ACTION_MODE_NOT_STARTED;
    private PageCitation mPageCitation;
    @Nullable
    private Highlight mSelectedHighlight = null;


    private SharedPreferences mSharedPref;
    private ViewGroup mPopupTextSelection;
    private boolean selectionEventTrack = false;

    private int mPagerPosition;
    private View mAactionDeleteHighlight;
    private View mActionAddComment;
    private boolean mIsPageBookmarked;
    private ViewStub mBookmarkFrame;
    private PageInfo pageInfo;
    private boolean tashkeelOn = true;
    private boolean pinchZoomOn;
    private BookInfo bookInfo;

    public BookPageFragment() {

    }

    @NonNull
    public static BookPageFragment newInstance(int bookId, int pageId, int pagerPosition) {
        Bundle bundle = new Bundle();
        bundle.putInt(BooksInformationDBContract.BooksAuthors.COLUMN_NAME_BOOK_ID, bookId);
        bundle.putInt(KEY_PAGER_POSITION, pagerPosition);
        bundle.putInt(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID, pageId);
        BookPageFragment bookPageFragment = new BookPageFragment();
        bookPageFragment.setArguments(bundle);
        return bookPageFragment;
    }

    private static int getCssColorInt(@ColorInt int color) {
        return 0x00FFFFFF & color;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        bookId = args.getInt(BooksInformationDBContract.BooksAuthors.COLUMN_NAME_BOOK_ID, 0);
        pageId = args.getInt(BookDatabaseContract.TitlesEntry.COLUMN_NAME_PAGE_ID, 0);
        mPagerPosition = args.getInt(KEY_PAGER_POSITION, 0);
        userDataDBHelper = UserDataDBHelper.getInstance(getContext(), bookId);
        try {
            BookDatabaseHelper bookDatabaseHelperInstance = BookDatabaseHelper.getInstance(getContext(), bookId);
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            page_content = bookDatabaseHelperInstance.getPageContentByPageId(pageId);
            mPageCitation = bookDatabaseHelperInstance.getCitationInformation(pageId);
            mPageCitation.setResources(getResources());
            pageInfo = mPageCitation.pageInfo;
            bookInfo = bookDatabaseHelperInstance.getBookInfo();
            setHasOptionsMenu(false);
        } catch (BookDatabaseException bookDatabaseException) {
            Timber.e(bookDatabaseException);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mIsPageBookmarked = userDataDBHelper.isPageBookmarked(pageId);
        if (mBookmarkFrame != null) {
            mBookmarkFrame.setVisibility(mIsPageBookmarked ? View.VISIBLE : View.GONE);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        FrameLayout rootView = (FrameLayout) inflater.inflate(R.layout.fragment_book_page, container, false);

        mBookPageWebView = rootView.findViewById(R.id.book_page_web_view);

        WebSettings webSettings = mBookPageWebView.getSettings();
        webSettings.setDefaultTextEncodingName("utf-8");
        mBookPageWebView.setVerticalScrollBarEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        mBookPageWebView.addJavascriptInterface(new WebAppInterface(), "selectioniterface");
        initializeWebView(mBookPageWebView, webSettings);

        final ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        final float scaleFactor = detector.getScaleFactor();
                        if (scaleFactor <= 0.05)
                            /*
                             the detector should consider this event
                            *  as not handled. If an event was not handled, the detector
                            *  will continue to accumulate movement until an event is
                            *  handled. This can be useful if an application, for example,
                            *  only wants to update scaling factors if the change is
                            *  greater than 0.05.
                             */
                            return false;
                        else {
                            int newZoom = (int) (webSettings.getTextZoom() * scaleFactor);
                            // Don't let the object get too small or too large.
                            newZoom = Math.max(AppConstants.DISPLAY_PREFERENCES_DEFAULTS.MIN_TEXT_ZOOM,
                                    Math.min(newZoom,
                                            AppConstants.DISPLAY_PREFERENCES_DEFAULTS.MAX_TEXT_ZOOM));
                            webSettings.setTextZoom(newZoom);

                            return true;
                        }
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        pageFragmentListener.onZoomChangedByPinch(webSettings.getTextZoom());
                    }
                });


        //region Touch Event Handling
        final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                WebView.HitTestResult hitResult = mBookPageWebView.getHitTestResult();
                if (hitResult != null && hitResult.getExtra() != null) {
                    // The click was on a link! Return false so to bypass processing.
                    return false;
                } else {
                    if (isTouchEventInBookmarkZone(e)) {
                        toggleBookmark();
                    } else {
                        onPageTapped();
                    }
                    return true;
                }

            }

            @Override
            public void onLongPress(MotionEvent e) {
//                selectionEventTrack=true;
//                View view=newSelectionRect(new Rect((int)e.getRawX(),(int)e.getRawY(),(int)e.getRawX()+50,(int)e.getRawY()+20));
//                animateShowView((View) view.getParent());
            }


        };


        final GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(getContext(), simpleOnGestureListener);

        mBookPageWebView.setOnTouchListener((v, e) -> {
//                final int action = e.getActionMasked();
//
//                switch (e.getAction() & MotionEvent.ACTION_MASK) {
//                    case MotionEvent.ACTION_DOWN:
//                        mDownX = e.getX();
//                        mDownY = e.getY();
//                        isOnClick = true;
//                        break;
//                    case MotionEvent.ACTION_CANCEL:
//                    case MotionEvent.ACTION_UP:
//                        if (isOnClick) {
//                            //TODO onClick code
//                            View view=newSelectionRect(new Rect((int)e.getRawX(),(int)e.getRawY(),(int)e.getRawX()+50,(int)e.getRawY()+20));
//                            animateShowView((View) view.getParent());
//                            selectionEventTrack=false;
//                        }
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//
//                        if (isOnClick && (Math.abs(mDownX - e.getX()) > SCROLL_THRESHOLD || Math.abs(mDownY - e.getY()) > SCROLL_THRESHOLD)) {
//                            isOnClick = false;
//                            if(selectionEventTrack)
//                            animateHidewView(getView().findViewById(R.id.selection_menu_card));
//
//                        }
//                        break;
//                    default:
//                        break;
//                }
            if (pinchZoomOn) {
                mScaleDetector.onTouchEvent(e);
            }
            return gestureDetectorCompat.onTouchEvent(e);

        });
        //endregion
        if (shouldDisplayFloatingSelectionMenu()) {
            initializeSelectionPopup(rootView);
        }

        mBookmarkFrame = rootView.findViewById(R.id.bookmark_view_stub);

        maybeUpdateViews();

        return rootView;
    }

    private void maybeUpdateViews() {
        if (this.bookInfo != null) {
            CharSequence title = bookInfo.getName();
            if (TextUtils.isEmpty(title)) {
                title = " ";
            }
            if (pageFragmentListener != null) {
                pageFragmentListener.populateReaderActionBar(title, bookInfo.getAuthorName());
            }

        }
    }

    private boolean isTouchEventInBookmarkZone(@NonNull MotionEvent e) {
        return (e.getX() < 250 && e.getY() < 300);
    }

    private void initializeSelectionPopup(@NonNull final View v) {
        mPopupTextSelection = v.findViewById(R.id.selection_popup_frame);
        mAactionDeleteHighlight = v.findViewById(R.id.highlight_remove);
        mActionAddComment = v.findViewById(R.id.action_add_comment);
        mPopupTextSelection.setAlpha(0);

        final View.OnClickListener mHighlightButtonOnClickListener = v1 -> onContextualMenuItemClicked(v1.getId());

        ViewGroup upperSelectionMenu = v.findViewById(R.id.text_selection_popup_0);
        for (int i = 0; i < upperSelectionMenu.getChildCount(); i++) {
            upperSelectionMenu.getChildAt(i).setOnClickListener(mHighlightButtonOnClickListener);
        }

        ViewGroup lowerSelectionMenu = v.findViewById(R.id.text_selection_popup_1);
        for (int i = 0; i < lowerSelectionMenu.getChildCount(); i++) {
            lowerSelectionMenu.getChildAt(i).setOnClickListener(mHighlightButtonOnClickListener);
        }
    }

    private void finishSelectionMenuOnButtonClicked() {
        if (shouldDisplayFloatingSelectionMenu())
            hideTextSelectionMenu();
        pageFragmentListener.finishActionMode();
        mIsInActionMode = ACTION_MODE_NOT_STARTED;
    }

    private void animateShowView(@NonNull View view) {
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1.0f);
    }

    private void animateHidewView(@NonNull final View view) {
        view.animate().alpha(0.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void onPageTapped() {
        if (mIsInActionMode == ACTION_MODE_NOT_STARTED) {
            pageFragmentListener.onPageTapped();
        } else if (mIsInActionMode == ACTION_MODE_DISMISSED)
            mIsInActionMode = ACTION_MODE_NOT_STARTED;
        else if (mIsInActionMode == ACTION_MODE_STARTED) {
            hideTextSelectionMenu();
            mIsInActionMode = ACTION_MODE_NOT_STARTED;
            selectionEventTrack = false;
        }

    }

    private View newSelectionRect(@NonNull Rect selectionRect) {
        View popup = getView().findViewById(R.id.selection_menu_card);
        preparePopuPosition(popup, selectionRect);
        return popup;
    }

    private void preparePopuPosition(@NonNull View popup, @NonNull Rect selectionRect) {
        preparePopuPosition(popup, selectionRect.left, selectionRect.bottom, selectionRect.top, selectionRect.right, 0);
    }

    void preparePopuPosition(@NonNull View view, int selectionLeft, int selectionBottom, int selectionTop, int selectionRight, int handleLongerAxis) {
        int drawLocationY;
        int drawLocationX;
        int size;
        int width;
        int height;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        size = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(size, size);
        height = view.getMeasuredHeight();
        width = view.getMeasuredWidth();
        drawLocationX = Math.min(
                screenWidth - width,
                Math.max(0,
                        (selectionLeft + ((selectionRight - selectionLeft) / 2)) - (width / 2)
                )
        );

        drawLocationY = selectionTop - height >= 0 ? selectionTop - height : (selectionBottom + height) + handleLongerAxis >= screenHeight ? handleLongerAxis > 0 ? selectionBottom - height : ((selectionBottom + selectionTop) / 2) - (height / 2) : selectionBottom + handleLongerAxis;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.leftMargin = drawLocationX;
        layoutParams.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.topMargin = drawLocationY;
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        view.setLayoutParams(layoutParams);
        //view.requestLayout();

    }

    private void showTextSelectionMenu() {
        if (shouldDisplayFloatingSelectionMenu()) {
            mBookPageWebView.loadUrl("javascript:getSelectionRect();");
            //  animateShowView(mPopupTextSelection);

        }
        mIsInActionMode = ACTION_MODE_STARTED;
    }

    private boolean shouldDisplayFloatingSelectionMenu() {
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        return false;
    }

    private void hideTextSelectionMenu() {
        mSelectedHighlight = null;
        mIsInActionMode = ACTION_MODE_DISMISSED;
        if (shouldDisplayFloatingSelectionMenu()) {
            animateHidewView(mPopupTextSelection);
            mAactionDeleteHighlight.setVisibility(View.INVISIBLE);
            mActionAddComment.setVisibility(View.INVISIBLE);
        } else {
            pageFragmentListener.finishActionMode();
        }
    }

    void highlightClicked(int highlightId) {

        try {
            mSelectedHighlight = userDataDBHelper.getHighlightById(highlightId, pageId);


            if (mSelectedHighlight.hasNote()) {
                //mPopupTextSelection.findViewById(R.id.action_add_comment).setVisibility(View.GONE);
                showNoteDialog();
            } else {
                showTextSelectionMenu();
                if (shouldDisplayFloatingSelectionMenu()) {
                    mAactionDeleteHighlight.setVisibility(View.VISIBLE);
                    mActionAddComment.setVisibility(View.VISIBLE);
                } else {
                    pageFragmentListener.startSelectionActionMode();
                }
            }
        } catch (BookDatabaseException e) {
            Timber.e(e);
        }

    }

    @Override
    public void onFinishHighlightNoteDialog(@NonNull String noteText) {
        if (mSelectedHighlight != null && !noteText.isEmpty()) {
            mSelectedHighlight.noteText = noteText;
            userDataDBHelper.addNoteToHighlight(mSelectedHighlight);
        }

    }

    private void showNoteDialog() {
        NotePopupFragment notePopupFragment = NotePopupFragment.newInstance(mSelectedHighlight);
        //see this answer http://stackoverflow.com/a/37794319/3061221
        FragmentManager fm = getChildFragmentManager();
        notePopupFragment.show(fm, "fragment_note");
    }

    private void initializeWebView(@NonNull WebView webView, @NonNull WebSettings webSettings) {
        tashkeelOn = pageFragmentListener.getTashkeelState();
        if (!tashkeelOn) page_content = ArabicUtilities.cleanTashkeel(page_content);

        boolean isNightMode = pageFragmentListener.isNightMode();

        int intialZoom = pageFragmentListener.getDisplayZoom();
        webSettings.setTextZoom(intialZoom);


        String data = prepareHtml(isNightMode);
        loadWebView(data, webView);
        if (isNightMode) webView.setBackgroundColor(Color.TRANSPARENT);

    }

    private String getHeadStyleCss(boolean isNightMode) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        if (pageFragmentListener != null) {
            formatter.format("<style>\n" +
                            "    body {\n" +
                            "      background-color: #%06X;\n" +
                            "      color: #%06X;\n" +
                            "    }\n" +
                            "    h1,h2,h3,h4,h5,h6 {\n" +
                            "      color: #%06X\n" +
                            "    }\n" +
                            "  </style>",
                    getCssColorInt(pageFragmentListener.getBackGroundColor()),
                    getCssColorInt(pageFragmentListener.getTextColor(isNightMode)),
                    getCssColorInt(pageFragmentListener.getHeadingColor(isNightMode))
            );
        }

        return sb.toString();


    }

    @NonNull
    private String prepareHtml(boolean isNightMode) {
        StringBuilder stringBuilder = new StringBuilder()
                .append("<!doctype html>")
                .append("<html align='justify' dir=\"rtl\">")
                .append("<head>")
                .append("<title>").append(bookInfo.getName()).append("</title>")
                .append(getHeadStyleCss(isNightMode))
                .append("</head>")
                .append("<body>")
                .append("<link href=\"styles/styles.css\" rel=\"stylesheet\" type=\"text/css\">")
                .append("<link href=\"styles/highlight.css\" rel=\"stylesheet\" type=\"text/css\">");

        if (isNightMode)
            stringBuilder.append("<link href=\"styles/ReadingNight.css\" rel=\"stylesheet\" type=\"text/css\">");

        stringBuilder
                .append(page_content)
                .append("</body>").append("<script src=\"scripts/jquery-3.2.1.min.js\"></script>")
                .append(footNoteScript()).append("<script src='scripts/rangy/rangy-core.js'></script>")
                .append("<script src='scripts/rangy/rangy-serializer.js'></script>")
                .append("<script src='scripts/rangy/rangy-highlighter.js'></script>")
                .append("<script src='scripts/rangy/rangy-classapplier.js'></script>")
                .append("<script src='scripts/rangy/rangy-textrange.js' defer></script>")
                .append("<script src='scripts/rangy/rangy-position.js' ></script>")
                .append("<script src='scripts/highlight.js'></script>")
                .append("</html>");


        return stringBuilder.toString();
    }

    private void loadWebView(String data, @NonNull WebView webView) {
        webView.loadDataWithBaseURL(
                ANDROID_ASSET,
                data,
                "text/html",
                "utf-8",
                null);
    }

    @NonNull
    private String footNoteScript() {
        return
                "<script>" +
                        "if ($(\".comment\").length)" +
                        "{" +
                        "$(\"body\").append(\"<hr>\");" +
                        "var footnote_id_int =1;" +
                        "var bookId=" +
                        bookId + ";" +
                        "var pageRowId=" + pageId + ";" +
                        "$(\".comment\").each(function() { " +
                        "var footnote_id  = 'footnote_' +bookId+\"_\"+pageRowId+\"_\"+ footnote_id_int;" +
                        "var text_reference_id = this.id =\"text_reference_\" +bookId+\"_\"+pageRowId+\"_\"+footnote_id_int;" +
                        "$(this).text('('+footnote_id_int+')');" +
                        "$(this).attr(\"href\", '#'+footnote_id);" +
                        "var footnote_text = $(this).attr('title');" +
                        "$( \"<a id=\" + footnote_id + \" href=#\" +text_reference_id +\">\" +'('+footnote_id_int+')'+\"</a>\"" +
                        "+\"<span> \"+\" \"+ footnote_text  + \"</span>\" + \"<br>\").appendTo( \"body\" );" +
                        " footnote_id_int++;" +
                        "});" + "}" +
                        "</script>";
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        if (activity instanceof PageFragmentListener) {
            pageFragmentListener = (PageFragmentListener) activity;
        }
        if (activity instanceof ReadingActivity) {
            ((ReadingActivity) activity).registerBottomToolBarActionListener(this);
            ((ReadingActivity) activity).registerActionModeChangeListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pageFragmentListener = null;
        ((ReadingActivity) getActivity()).unregisterActionModeChangeListener(this);
        ((ReadingActivity) getActivity()).unregisterBottomToolBarActionListener(this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.book_page_fragment, menu);
        MenuItem bookmarkItem = menu.findItem(R.id.action_bookmark_this_page);
        bookmarkItem.setChecked(mIsPageBookmarked);
        setBookMarkIcon(bookmarkItem);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bookmark_this_page: {
                changeBookMarkState(!item.isChecked());
                item.setChecked(!item.isChecked());
                setBookMarkIcon(item);
                return true;
            }
            default:
                return false;
        }

    }

    void toggleBookmark() {
        boolean bookmarkExists = userDataDBHelper.isPageBookmarked(pageId);
        boolean newBookmarkState = !bookmarkExists;
        changeBookMarkState(newBookmarkState);

        if (pageFragmentListener != null) {
            pageFragmentListener.setBookmarkState(newBookmarkState);
        }
    }

    public void onBookmarkStateChange(boolean newBookmarkState, int pageId) {
        if (pageId == this.pageId) {
            changeBookMarkState(newBookmarkState);
        }
    }

    void changeBookMarkState(boolean newBookmarkState) {

        if (newBookmarkState) {
            userDataDBHelper.addBookmark(pageId);
            mBookmarkFrame.setVisibility(View.VISIBLE);
            View bookmarkImage = getView().findViewById(R.id.bookmark_icon);
            AnimationUtils.addBookmarkWithAnimation(bookmarkImage, new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });


        } else {
            View bookmarkImage = getView().findViewById(R.id.bookmark_icon);

            AnimationUtils.removeBookmarkWithAnimation(bookmarkImage, new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mBookmarkFrame.setVisibility(View.INVISIBLE);
                    userDataDBHelper.RemoveBookmark(pageId);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        mIsPageBookmarked = newBookmarkState;

    }

    private void setBookMarkIcon(@NonNull MenuItem item) {
        TypedValue typedvalueattr = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.menuBookmarkIcon, typedvalueattr, true);
        StateListDrawable stateListDrawable = (StateListDrawable) getResources().getDrawable(typedvalueattr.resourceId);
        int[] state = {item.isChecked() ? android.R.attr.state_checked : -android.R.attr.state_checked};
        stateListDrawable.setState(state);
        item.setIcon(stateListDrawable.getCurrent());
        item.setTitle(item.isChecked() ? R.string.action_remove_book_mark : R.string.action_add_book_mark);
    }


    @Override
    public void actionModeStarted() {
        showTextSelectionMenu();
    }

    @Override
    public void actionModeFinished() {
        if (shouldDisplayFloatingSelectionMenu())
            hideTextSelectionMenu();
    }

    @Override
    public void onContextualMenuItemClicked(int itemId, int pagerPosition) {
        if (pagerPosition == mPagerPosition) {
            onContextualMenuItemClicked(itemId);
        }
    }


    public void onContextualMenuItemClicked(int itemId) {
        switch (itemId) {
            case R.id.action_copy_text:
                mBookPageWebView.loadUrl("javascript:copySelectedText();");
                finishSelectionMenuOnButtonClicked();
                break;
            case R.id.action_share_text:
                mBookPageWebView.loadUrl("javascript:shareSelectedText();");
                finishSelectionMenuOnButtonClicked();
                break;
            case R.id.action_select_all_text:
                mBookPageWebView.loadUrl("javascript:selectAll();");
                break;
            case R.id.action_add_comment:
                if (mSelectedHighlight != null) {
                    showNoteDialog();
                }
                break;
            case R.id.highlight_1:
                mBookPageWebView.loadUrl("javascript:highlightSelectedText(1);");
                mBookPageWebView.loadUrl("javascript:serializeHighlights();");
                finishSelectionMenuOnButtonClicked();
                break;
            case R.id.highlight_2:
                mBookPageWebView.loadUrl("javascript:highlightSelectedText(2);");
                mBookPageWebView.loadUrl("javascript:serializeHighlights();");
                finishSelectionMenuOnButtonClicked();
                break;
            case R.id.highlight_3:
                mBookPageWebView.loadUrl("javascript:highlightSelectedText(3);");
                mBookPageWebView.loadUrl("javascript:serializeHighlights();");
                finishSelectionMenuOnButtonClicked();
                break;
            case R.id.highlight_4:
                mBookPageWebView.loadUrl("javascript:highlightSelectedText(4);");
                mBookPageWebView.loadUrl("javascript:serializeHighlights();");
                finishSelectionMenuOnButtonClicked();
                break;
            case highlight_remove:
                mBookPageWebView.loadUrl("javascript:removeHighlightFromSelectedText();");
                mBookPageWebView.loadUrl("javascript:serializeHighlights();");
                finishSelectionMenuOnButtonClicked();
                break;

        }

    }


    private String addCitationToString(String rawText) {
        return rawText + mPageCitation.getCitationString();
    }

    private String addCitationToHTML(String rawHTML) {
        return rawHTML + mPageCitation.getCitationHtml();
    }

    @Override
    public void setZoom(int newZoom) {
        WebSettings webSettings = mBookPageWebView.getSettings();
        if (newZoom != webSettings.getTextZoom())
            webSettings.setTextZoom(newZoom);
    }

    @Override
    public void setTashkeel(boolean tashkeelOn) {
        if (this.tashkeelOn != tashkeelOn) {
            reloadeWithTashkeelOn(tashkeelOn);
        }
    }

    @Override
    public void setPinchZoom(boolean pinchZoomOn) {
        this.pinchZoomOn = pinchZoomOn;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBookPageWebView.loadUrl(String.
                format(Locale.US,
                        "javascript:setBackgroundColor('#%06X');", 0xFFFFFF & color));
    }

    @Override
    public void setHeadingColor(int color) {
        mBookPageWebView.loadUrl(String.
                format(Locale.US,
                        "javascript:setHeadingColor('#%06X');", 0xFFFFFF & color));
    }

    @Override
    public void setTextColor(int color) {
        mBookPageWebView.loadUrl(String.
                format(Locale.US,
                        "javascript:setTextColor('#%06X');", 0xFFFFFF & color));
    }

    @Override
    public void highLightSearchResult(@NonNull String searchQuery) {
        mBookPageWebView.loadUrl(String.
                format(Locale.US,
                        "javascript:highlightSearchString('%s');", searchQuery));
    }

    @Override
    public void removeSearchResultHighlights() {
        mBookPageWebView.loadUrl("javascript:removeSearchResultHighlights();");
    }

    private void reloadeWithTashkeelOn(boolean tashkeelOn) {
        if (this.tashkeelOn != tashkeelOn) {
            if (this.tashkeelOn) {
                page_content = ArabicUtilities.cleanTashkeel(page_content);
            } else {
                BookDatabaseHelper bookDatabaseHelperInstance = null;
                try {
                    bookDatabaseHelperInstance = BookDatabaseHelper.getInstance(getContext(), bookId);

                    page_content = bookDatabaseHelperInstance.getPageContentByPageId(pageId);
                } catch (BookDatabaseException e) {
                    Timber.e(e);
                }
            }
            initializeWebView(mBookPageWebView, mBookPageWebView.getSettings());
            this.tashkeelOn = tashkeelOn;
        }
    }


    // Container Activity must implement this interface
    public interface PageFragmentListener {
        void onPageTapped();

        void finishActionMode();

        void onZoomChangedByPinch(int newZoom);

        void startSelectionActionMode();

        void setBookmarkState(boolean Checked);

        boolean isNightMode();

        int getDisplayZoom();

        boolean getTashkeelState();

        @ColorInt
        int getBackGroundColor();

        @ColorInt
        int getHeadingColor();

        @ColorInt
        int getHeadingColor(boolean isNightMode);

        @ColorInt
        int getTextColor();

        @ColorInt
        int getTextColor(boolean isNightMode);

        void populateReaderActionBar(CharSequence title, CharSequence author);

        @Nullable
        String getSearchStringQuery();
    }


    public class WebAppInterface {


        private Handler handler;

        WebAppInterface() {

            this.handler = new Handler();
        }

        @JavascriptInterface
        public String getSerializedHighlights() {
            return userDataDBHelper.getSerializedHighlights(pageId);
        }

        @JavascriptInterface
        public void setSerializedHighlights(String serializedHighlights) {
            handler.post(() -> userDataDBHelper.setSerializedHighlights(pageInfo, serializedHighlights));

        }

        @JavascriptInterface
        public void copySelectedText(String str, String htmlStr) {
            ClipboardManager clipboard = (ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newHtmlText("simple text", addCitationToString(str), addCitationToHTML(htmlStr));
            clipboard.setPrimaryClip(clip);

        }

        @JavascriptInterface
        public void shareSelectedText(String str, String htmlStr) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, addCitationToString(str));
            sendIntent.putExtra(Intent.EXTRA_HTML_TEXT, addCitationToHTML(htmlStr));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, null));
        }

        @JavascriptInterface
        public void jsDebug(String error) {
            Log.d("WebAppInterface", "JSError: " + error);
        }

        @JavascriptInterface
        public void highlightClicked(final int highlightId) {
            Log.d(getString(R.string.sd), getString(R.string.ddd) + highlightId);
            handler.post(() -> BookPageFragment.this.highlightClicked(highlightId));

        }

        @JavascriptInterface
        public String getSearchQuery() {
            return pageFragmentListener != null ? pageFragmentListener.getSearchStringQuery() : null;
        }

        @JavascriptInterface
        public void setSelectionRect(int left, int top, int right, int bottom) {
            final Rect selectionRect = new Rect(left, top, right, bottom);
            handler.post(() -> BookPageFragment.this.newSelectionRect(selectionRect));

        }

    }


}
