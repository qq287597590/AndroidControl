package com.meizu.flyme.flymebbs.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.meizu.common.widget.PullRefreshLayout;
import com.meizu.flyme.flymebbs.R;
import com.meizu.flyme.flymebbs.adapter.BaseRecyclerViewAdapter;
import com.meizu.flyme.flymebbs.utils.LogUtils;
import com.meizu.flyme.flymebbs.utils.NetWorkUtil;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import flyme.support.v7.widget.LinearLayoutManager;
import flyme.support.v7.widget.MzRecyclerView;
import flyme.support.v7.widget.RecyclerView;

/**
 * User: wangxiaolong
 * Date: 2015-09-12
 * Time: 09:51
 * Description:
 */
public class RefreshRecyclerView extends MzRecyclerView {
    OnLoadMoreListener mOnLoadMoreListener = null;
    private View mEmptyView = null;
    private View mLoadingView = null;
    private View mNoNetView = null;
    onScrollStateChangedListener mOnScrollStateChangedListener = null;
    private ScrollListener mScrollListener = null;
    SwipeRefreshLayout mHeaderView = null;
    public RelativeLayout mFooterView;
    private LinearLayoutManager mLayoutManager;
    private boolean isLoadMoreDone = true;
    private boolean isShowFooterView = true;
    private int mFooterHeight = 0;
    private boolean firstRefreshing = false;
    private FrameLayout mFrameLayout;
    Handler mHandler = new Handler(Looper.getMainLooper());
    private Adapter mAdapter;
    private View mRecyclerViewEmptyView;
    final private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };
    void checkIfEmpty() {
        if (mRecyclerViewEmptyView != null) {
            final boolean emptyViewVisible = getAdapter() == null||getAdapter().getItemCount() == 1;
            mRecyclerViewEmptyView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
        }

    }
    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
        checkIfEmpty();
    }

    public void setEmptyView(View emptyView) {
        this.mRecyclerViewEmptyView = emptyView;
        checkIfEmpty();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.mOnLoadMoreListener = onLoadMoreListener;
    }

    public void setOnScrollStateChangedListener(onScrollStateChangedListener onScrollStateChangedListener) {
        this.mOnScrollStateChangedListener = onScrollStateChangedListener;
    }

    public void setScrollListener(ScrollListener scrollListener) {
        this.mScrollListener = scrollListener;
    }

    public void setHeader(SwipeRefreshLayout swipeRefreshLayout, View emptyView, View loadingView, View noNetView) {
        mHeaderView = swipeRefreshLayout;
        mEmptyView = emptyView;
        mLoadingView = loadingView;
        mNoNetView = noNetView;
        mHeaderView.setColorSchemeResources(android.R.color.black, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        mHeaderView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!NetWorkUtil.isNetworkConnected(getContext())) {
                    SnackbarManager.show(Snackbar.with(getContext()).text(getResources().getString(R.string.network_error)));
                    mHeaderView.setRefreshing(false);
                    return;
                }
                mOnLoadMoreListener.onPullDown2Refresh();
            }
        });
    }
    public void notifyFirstRefresh() {
        firstRefreshing = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (firstRefreshing) {                            //still refreshing after 500 ms
                    enableEmptyView(mLoadingView);
                }
            }
        }, 500);
    }
    public void enableEmptyView(View view) {
        if (mEmptyView != null)
            mEmptyView.setVisibility(View.GONE);
        if (mNoNetView != null)
            mNoNetView.setVisibility(View.GONE);
        if (mLoadingView != null)
            mLoadingView.setVisibility(View.GONE);
        if (view != null)
            setEmptyView(view);

    }
    public void onLoadMoreDone() {
        if (mFooterView != null && !isLoadMoreDone) {
            //mFooterView.setVisibility(View.GONE);
            isLoadMoreDone = true;
            isShowFooterView = true;
        }
    }

    public void onLoadNoMoreDone() {
        if (null != mFooterView) {
            if (isShowFooterView && !isLoadMoreDone) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        collapse();
                    }
                }, 100);
            }
            if (!isShowFooterView && !isLoadMoreDone) {
                isLoadMoreDone = true;
            }
        }
    }

    public void onLoadRefreshDone() {
        if (firstRefreshing) {
            firstRefreshing = false;
        }
        if (mHeaderView != null && mHeaderView.isRefreshing()) {
            mHeaderView.setRefreshing(false);
        }
        checkIfEmpty();
    }

    Animation.AnimationListener al = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mFooterView.setVisibility(View.GONE);
            mFrameLayout.getLayoutParams().height = -2;
            mFrameLayout.requestLayout();
            isShowFooterView = false;
            isLoadMoreDone = true;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private synchronized void collapse() {
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1.0f) {
                    mFooterView.setVisibility(View.GONE);
                } else {
                    mFrameLayout.getLayoutParams().height = mFooterHeight - (int) (mFooterHeight * interpolatedTime);
                    mFrameLayout.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        if (al != null) {
            animation.setAnimationListener(al);
        }
        animation.setDuration(600);
        if (mFrameLayout.getAnimation() != null)
            mFrameLayout.clearAnimation();
        mFrameLayout.startAnimation(animation);
    }


    public void setAdapter(BaseRecyclerViewAdapter adapter) {
        super.setAdapter(adapter);
        mAdapter = adapter;
        mFooterView = (RelativeLayout) adapter.getFooterView();
        if (mFooterView != null) {
            mFrameLayout = (FrameLayout) mFooterView.getParent();
            mFooterView.measure(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
            mFooterHeight = mFooterView.getMeasuredHeight();
        }

    }

    public RefreshRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context) {
        mLayoutManager = new LinearLayoutManager(context);
        setLayoutManager(mLayoutManager);
        setHasFixedSize(true);
        setOnScrollListener(onScrollListener);
        setDelayTopOverScrollEnabledReflect(this, false);    //关闭下拉悬停
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    //interface is offered to Presenter
    public interface OnLoadMoreListener {
        void onPullUp2LoadMore();

        void onPullDown2Refresh();
    }

    public interface onScrollStateChangedListener {
        void onScrollStateChanged(RecyclerView view, int scrollState);
    }

    public interface ScrollListener {
        void onScrolled(RecyclerView recyclerView, int dx, int dy);
    }


    private OnScrollListener onScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                int flag = 1;
                if(null != mFooterView && mFooterView.getVisibility() == GONE)
                    flag = 2;
                //加载更多功能的代码
                //判断是否滚动到底部并且加载更多已完成
                if (null != mAdapter) {
                    if ((mLayoutManager.findLastVisibleItemPosition() == mAdapter.getItemCount() - flag) && isLoadMoreDone && recyclerView.getChildCount()>1) {
                        if (mFooterView != null && isShowFooterView && isLoadMoreDone && mFooterHeight != 0) {
                            mFooterView.setVisibility(View.VISIBLE);
                        }
                        if (mOnLoadMoreListener != null) {
                            isLoadMoreDone = false;
                            mOnLoadMoreListener.onPullUp2LoadMore();
                        }
                    }
                }
            }

            if (mOnScrollStateChangedListener != null) {//接口回调
                mOnScrollStateChangedListener.onScrollStateChanged(recyclerView, newState);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (mScrollListener != null) {
                mScrollListener.onScrolled(recyclerView, dx, dy);
            }
        }
    };


    private static void setDelayTopOverScrollEnabledReflect(RefreshRecyclerView listview, boolean enable) {
        try {
            Method method = Class.forName("android.widget.AbsListView")
                    .getMethod("setDelayTopOverScrollEnabled",
                            boolean.class);
            try {
                method.invoke(listview, enable);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (SecurityException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void setIsShownFooterView(boolean value){
        this.isShowFooterView = value;
    }


}
