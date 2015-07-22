package com.meizu.flyme.flymebbs.widget;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.meizu.flyme.flymebbs.R;
import com.meizu.flyme.flymebbs.utils.NetWorkUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by wangxiaolong on 15-6-16.
 */

public class RefreshListView extends ListView implements AbsListView.OnScrollListener {

    ProgressBar mFooterView = null;
    OnLoadMoreListener mOnLoadMoreListener = null;
    onScrollStateChangedListener mOnScrollStateChangedListener=null;
    SwipeRefreshLayout mHeaderView = null;

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.mOnLoadMoreListener = onLoadMoreListener;
    }

    public void setOnScrollStateChangedListener(onScrollStateChangedListener onScrollStateChangedListener){
        this.mOnScrollStateChangedListener=onScrollStateChangedListener;
    }

    public void setHeader(SwipeRefreshLayout swipeRefreshLayout) {
        mHeaderView = swipeRefreshLayout;
        mHeaderView.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        mHeaderView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(!NetWorkUtil.isNetworkConnected(getContext())){
                    Toast.makeText(getContext(),getContext().getString(R.string.flymebbs_net_faild),Toast.LENGTH_SHORT).show();
                    mHeaderView.setRefreshing(false);
                    return;
                }
                mOnLoadMoreListener.onLoadRefresh();
            }
        });

    }

    public void onLoadMoreDone() {
        if (mFooterView != null)
            mFooterView.setVisibility(View.GONE);
    }

    public void onLoadRefreshDone() {
        if(mHeaderView != null)
            mHeaderView.setRefreshing(false);
    }

    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
    }

    public RefreshListView(Context context) {
        super(context);
        init(context);
    }

    public RefreshListView(Context context, AttributeSet paramAttributeSet) {
        super(context, paramAttributeSet);
        init(context);
    }

    public RefreshListView(Context context, AttributeSet paramAttributeSet, int paramInt) {
        super(context, paramAttributeSet, paramInt);
        init(context);
    }

    public void init(Context context) {
        mFooterView = new ProgressBar(context);
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.addView(mFooterView, 0, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        addFooterView(frameLayout);
        mFooterView.setVisibility(View.GONE);
        setOnScrollListener(this);
        setDelayTopOverScrollEnabledReflect(this, false);    //关闭下拉悬停
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    public void onScroll(AbsListView paramAbsListView, int firstVisibleItem, int paramInt2, int paramInt3) {

    }
    //interface is offered to Presenter
    public interface OnLoadMoreListener {
        void onLoadMore();
        void onLoadRefresh();
    }

    public interface onScrollStateChangedListener{
        void onScrollStateChanged(AbsListView view, int scrollState);
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            // 判断是否滚动到底部
            if (view.getLastVisiblePosition() == view.getCount() - 1) {
                //加载更多功能的代码
                if (mFooterView != null)
                    mFooterView.setVisibility(View.VISIBLE);
                if (mOnLoadMoreListener != null)
                    mOnLoadMoreListener.onLoadMore();
            }
        }

        if(mOnScrollStateChangedListener!=null){//接口回调
            mOnScrollStateChangedListener.onScrollStateChanged(view, scrollState);
        }
    }

    private static void setDelayTopOverScrollEnabledReflect(RefreshListView listview, boolean enable) {
        try {
            Method method = Class.forName("android.widget.AbsListView")
                    .getMethod("setDelayTopOverScrollEnabled",
                            new Class[]{boolean.class});
            try {
                method.invoke(listview, enable);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
