/*
 * Copyright 2011 tfdroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bugly.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class MyScrollScreen extends ViewGroup {

    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMaximumVelocity;
    //private int mDefaultScreen;
    private int mCurrentScreen;
    private boolean mFirstLayout = true;

    private float mLastMotionX;
    private int mTouchState = TOUCH_STATE_REST;
    private OnScreenChangeListener mOnScreenChangeListener;

    private static final int DEFAULT_SCREEN = 0;
    private static final int SNAP_VELOCITY = 800;
    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    private final static double SLOP_RATE = 3;	//最小点数的倍数
    private final static double SCROLL_DURATION_RATE = 0.75;	//越小，越快
    private final static int SCROLL_DURATION = 500;	//毫秒

    private int SCREEN_WIDTH;	//初始化时记录屏幕宽度，以后不再获取以加快速度
    private int SCROLL_DIFF; //屏幕宽度的1/10，滑动到头后，允许的弹性

    public MyScrollScreen(Context context, AttributeSet attrs) {
        super(context, attrs);

        //TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollScreen);
        //mDefaultScreen = a.getInt(R.styleable.ScrollScreen_default_screen, DEFAULT_SCREEN);
        //a.recycle();
        //mDefaultScreen = DEFAULT_SCREEN;

        initScrollScreen();
    }

    private void initScrollScreen() {
        Context context = getContext();
        mScroller = new Scroller(context);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        //zcx change
        //mTouchSlop = configuration.getScaledTouchSlop();
        mTouchSlop = (int) (configuration.getScaledTouchSlop() * SLOP_RATE);

        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mCurrentScreen = DEFAULT_SCREEN;//mDefaultScreen;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

        if (mFirstLayout) {
            setHorizontalScrollBarEnabled(false);
            scrollTo(mCurrentScreen * width, 0);
            setHorizontalScrollBarEnabled(true);
            mFirstLayout = false;
            //zcx add
            SCREEN_WIDTH = width;
            SCROLL_DIFF = 0;//width / 10;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child == null) {
                return;
            }
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    public void computeScroll() {///不同
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final float x = ev.getX();
                final int xDiff = (int) Math.abs(x - mLastMotionX);

                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;

                if (xMoved) {
                    mTouchState = TOUCH_STATE_SCROLLING;
                    mLastMotionX = x;
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                mLastMotionX = x;

                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouchState = TOUCH_STATE_REST;

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;
        }

        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                mLastMotionX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:///不同
                final float x = ev.getX();
                final float deltaX = mLastMotionX - x;
                mLastMotionX = x;

                //zcx add,滑到左右边界时有所限制
                final int childCount = getChildCount();

                if (mCurrentScreen == 0 && deltaX < 0) {	//最左屏，且试图右滑
                    final int scrollX = getScrollX();
                    if ((scrollX+deltaX) > -SCROLL_DIFF) {
                        scrollBy((int) deltaX, 0);
                    }
                    else if (scrollX > -SCROLL_DIFF) {
                        scrollBy(-scrollX - SCROLL_DIFF, 0);
                    }
                }
                else if (mCurrentScreen == childCount-1 && deltaX > 0) {	//最右屏，且试图左滑
                    final int scrollX = getScrollX();
                    final int maxDiff = (childCount-1)*SCREEN_WIDTH + SCROLL_DIFF;
                    if ((scrollX + deltaX) < maxDiff) {	//移动后不会越界
                        scrollBy((int) deltaX, 0);
                    }
                    else if (scrollX < maxDiff) {		//现在不越界，但移动后会越界
                        scrollBy(maxDiff - scrollX, 0);
                    }
                }
                else {	//正常移动
                    scrollBy((int) deltaX, 0);
                }

                break;
            case MotionEvent.ACTION_UP:///不同
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final int velocityX = (int) velocityTracker.getXVelocity();

                if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
                    snapToScreen(mCurrentScreen - 1);
                } else if (velocityX < -SNAP_VELOCITY && mCurrentScreen < getChildCount() - 1) {
                    snapToScreen(mCurrentScreen + 1);
                } else {
                    snapToDestination();
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mTouchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
                break;
        }

        return true;
    }

    public void snapToDestination() {
        final int screenWidth = getWidth();
        final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
        snapToScreen(destScreen);
    }

    private void snapToScreen(int whichScreen) {
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));

        if (getScrollX() != (whichScreen * getWidth())) {
            final int delta = whichScreen * getWidth() - getScrollX();
            //zcx change
            mScroller.startScroll(getScrollX(), 0, delta, 0, (int) (Math.abs(delta) * SCROLL_DURATION_RATE));
            //mScroller.startScroll(getScrollX(), 0, delta, 0, SCROLL_DURATION);

            mCurrentScreen = whichScreen;
            invalidate();

            invokeOnScreenChangeListener();
        }
    }

    public void addScreen(View view) {
        addView(view,new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public int getCurrentScreen() {
        return mCurrentScreen;
    }

    public void setToScreen(int whichScreen) {
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        mCurrentScreen = whichScreen;
        //zcx change, 否则，点击indicator时，没有滑动效果
        //scrollTo(whichScreen * getWidth(), 0);
        snapToScreen(whichScreen);
    }

    public void setOnScreenChangedListener(OnScreenChangeListener l) {
        mOnScreenChangeListener = l;
    }

    private void invokeOnScreenChangeListener() {
        if (mOnScreenChangeListener != null) {
            mOnScreenChangeListener.onScreenChanged(getCurrentScreen());
        }
    }

    public interface OnScreenChangeListener {

        void onScreenChanged(int index);
    }
}
