package com.google.developer.udacityalumni.article;


import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.developer.udacityalumni.R;
import com.google.developer.udacityalumni.adapter.ArticleAdapter;
import com.google.developer.udacityalumni.data.AlumContract;
import com.google.developer.udacityalumni.view.slidingview.AvatarCardAdapter;
import com.google.developer.udacityalumni.view.slidingview.SlidingViewManager;


public class ArticleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String[] ARTICLE_COLUMNS = {AlumContract.ArticleEntry._ID,
            AlumContract.ArticleEntry.COL_ARTICLE_ID, AlumContract.ArticleEntry.COL_TITLE,
            AlumContract.ArticleEntry.COL_SPOTLIGHTED, AlumContract.ArticleEntry.COL_CONTENT,
            AlumContract.ArticleEntry.COL_IMAGE, AlumContract.ArticleEntry.COL_SLUG,
            AlumContract.ArticleEntry.COL_USER_ID, AlumContract.ArticleEntry.COL_USER_NAME,
            AlumContract.ArticleEntry.COL_USER_AVATAR, AlumContract.ArticleEntry.COL_CREATED_AT,
            AlumContract.ArticleEntry.COL_UPDATED_AT, AlumContract.ArticleEntry.COL_RANDOM_TAG_ID,
            AlumContract.ArticleEntry.COL_RANDOM_TAG, AlumContract.ArticleEntry.COL_BOOKMARKED,
            AlumContract.ArticleEntry.COL_FOLLOWING_AUTHOR};

    public static final int IND_ID = 0;
    public static final int IND_ARTICLE_ID = 1;
    public static final int IND_TITLE = 2;
    public static final int IND_SPOTLIGHTED = 3;
    public static final int IND_CONTENT = 4;
    public static final int IND_IMAGE = 5;
    public static final int IND_SLUG = 6;
    public static final int IND_USER_ID = 7;
    public static final int IND_USER_NAME = 8;
    public static final int IND_USER_AVATAR = 9;
    public static final int IND_CREATED_AT = 10;
    public static final int IND_UPDATED_AT = 11;
    public static final int IND_RANDOM_TAG_ID = 12;
    public static final int IND_RANDOM_TAG = 13;
    public static final int IND_BOOKMARKED = 14;
    public static final int IND_FOLLOWING_AUTHOR = 15;

    private static final int ARTICLE_LOADER = 100;

    private ArticleAdapter mArticleAdapter;
    private boolean mIsBookmarked;

    private SlidingViewManager mManager;
    private AvatarCardAdapter mAvatarCardAdapter;

    public ArticleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_article, container, false);
        RecyclerView mRecyclerView = rootView.findViewById(R.id.article_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);

        final AvatarCardAdapter.OnClickListener listener = new AvatarCardAdapter.OnClickListener() {
            @Override
            public void onSeeMoreClick() {
                Toast.makeText(getContext(), "See More Clicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDismissClick() {
                mManager.animate();
            }
        };

        mManager = new SlidingViewManager(this);
        mAvatarCardAdapter = new AvatarCardAdapter(listener);
        mManager.setAdapter(mAvatarCardAdapter);
        if(savedInstanceState != null) {
            mManager.onRestoreInstanceState(savedInstanceState);
        }

        mArticleAdapter = new ArticleAdapter(getContext(), new ArticleAdapter.ArticleItemClickHandler() {

            @Override
            public void onArticleClick(long articleId, boolean isBookmarked, String tag) {
                ((ArticleCallback) getActivity()).onArticleSelected(articleId, isBookmarked,tag);
            }

            @Override
            public void onProfPicClick(long userId, int position) {
                //TODO: Pull up users profile from bottom pane: https://github.com/BenGoBlue05/UdacityAlumni/blob/master/Todos/prof-pic-click.md
                final Cursor c = mArticleAdapter.getCursor();
                c.moveToPosition(position);

                final String name = c.getString(ArticleFragment.IND_USER_NAME);
                final String image = c.getString(ArticleFragment.IND_USER_AVATAR);
                final String content = c.getString(ArticleFragment.IND_TITLE);

                mAvatarCardAdapter.setName(name);
                mAvatarCardAdapter.setImageUri(Uri.parse(image));
                mAvatarCardAdapter.setContent(content);

                mManager.animate();

            }

            @Override
            public void onFollowUserClick(long userId, long articleId, boolean wasFollowingBeforeClick, ImageView icon) {
                icon.setImageResource(!wasFollowingBeforeClick ? R.drawable.ic_following : R.drawable.ic_add_follow);
                ContentValues values = new ContentValues();
                values.put(AlumContract.ArticleEntry.COL_FOLLOWING_AUTHOR, !wasFollowingBeforeClick ? 1 : 0);
                getContext().getContentResolver().update(AlumContract.ArticleEntry.buildUriWithId(articleId),
                        values, null, null);
            }

            @Override
            public void onShareClick(String title) {
                title = title.toLowerCase();
                String url = "https://udacity-alumni-client.herokuapp.com/articles/" + title.replaceAll("\\s", "-");
//                ie: https://udacity-alumni-client.herokuapp.com/articles/medically-necessary-utilization-review-evidence-of-insurability
//                TODO: Share DYNAMIC link to the article  (the 'url' above is a link to the article on the web item_app)
//               Will implement later
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT,title);
                    i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share, url));
                    startActivity(Intent.createChooser(i,getString(R.string.choose_to)));

            }

            @Override
            public void onBookmarkClick(long articleId, boolean wasBookmarkedBeforeClick, ImageView icon) {
                icon.setImageResource(!wasBookmarkedBeforeClick ? R.drawable.ic_bookmark :
                        R.drawable.ic_bookmark_outline);
                mIsBookmarked = !wasBookmarkedBeforeClick;
                ContentValues values = new ContentValues();
                values.put(AlumContract.ArticleEntry.COL_BOOKMARKED, !wasBookmarkedBeforeClick ? 1 : 0);
                getContext().getContentResolver().update(AlumContract.ArticleEntry.buildUriWithId(articleId),
                        values, null, null);
            }

        });

        mRecyclerView.setAdapter(mArticleAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(ARTICLE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mManager.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AlumContract.ArticleEntry.CONTENT_URI, ARTICLE_COLUMNS,
                null, null, AlumContract.ArticleEntry.COL_CREATED_AT + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mArticleAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mArticleAdapter.swapCursor(null);
    }

    public interface ArticleCallback {
        void onArticleSelected(long articleId, boolean isBookmarked, String tag);
    }

}
