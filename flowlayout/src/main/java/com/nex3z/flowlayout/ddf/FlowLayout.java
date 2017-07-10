package com.nex3z.flowlayout.ddf;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.nex3z.flowlayout.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ddf on 17/7/4.
 */

public class FlowLayout extends ViewGroup {


    private static final int DEFAULT_CHILD_SPACING = 0;
    private static final float DEFAULT_ROW_SPACING = 0;
    private static final int DEFAULT_MAX_ROWS = Integer.MAX_VALUE;


    private int mChildSpacing = DEFAULT_CHILD_SPACING;
    private float mRowSpacing = DEFAULT_ROW_SPACING;
    private int mMaxRows = DEFAULT_MAX_ROWS;

    private List<Integer> mHeightForRowLst = new ArrayList<>();
    private List<Integer> mChildNumForRowLst = new ArrayList<>();

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout, 0, 0);
        mChildSpacing = a.getInt(R.styleable.FlowLayout_childSpacing, DEFAULT_CHILD_SPACING);

        try {
            try {
                mChildSpacing = a.getInt(R.styleable.FlowLayout_childSpacing, DEFAULT_CHILD_SPACING);
            } catch (NumberFormatException e) {
                mChildSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_childSpacing, (int) dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mRowSpacing = a.getInt(R.styleable.FlowLayout_rowSpacing, 0);
            }  catch (NumberFormatException e) {
                mRowSpacing = a.getDimension(R.styleable.FlowLayout_rowSpacing, dpToPx(DEFAULT_ROW_SPACING));
            }
            mMaxRows = a.getInt(R.styleable.FlowLayout_maxRows, DEFAULT_MAX_ROWS);
        } finally {
            a.recycle();
        }
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int measuredWidth = 0;
        int measuredHeight = 0;

        final int rowMaxWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int rowCurWidth = 0;    //当前行宽
        int rowCurHeight = 0;   //当前行高
        int childNumInRow = 0;  //一行child数量

        mChildNumForRowLst.clear();
        mHeightForRowLst.clear();

        int childSize = getChildCount();
        for (int i = 0; i < childSize; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams childLP = (MarginLayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth() + childLP.leftMargin + childLP.rightMargin;
            int childHeight = child.getMeasuredHeight() + childLP.topMargin + childLP.bottomMargin;

            // 需换行
            if (rowCurWidth + childWidth > rowMaxWidth) {
                // Save parameters for current row
                mChildNumForRowLst.add(childNumInRow);
                mHeightForRowLst.add(rowCurHeight);
                if (mChildNumForRowLst.size() <= mMaxRows) {
                    measuredHeight += rowCurHeight;
                }
                measuredWidth = Math.max(measuredWidth, rowCurWidth);

                // Place the child view to next row
                rowCurWidth = childWidth + mChildSpacing;
                childNumInRow = 1;
                rowCurHeight = childHeight;
            } else {
                childNumInRow++;
                rowCurWidth += childWidth + mChildSpacing;
                rowCurHeight = Math.max(rowCurHeight, childHeight);
            }
        }

        // Save parameters for current row
        mChildNumForRowLst.add(childNumInRow);
        mHeightForRowLst.add(rowCurHeight);
        if (mChildNumForRowLst.size() <= mMaxRows) {
            measuredHeight += rowCurHeight;
        }
        measuredWidth = Math.max(measuredWidth, rowCurWidth);

        measuredWidth += getPaddingLeft() + getPaddingRight();
        measuredHeight += getPaddingTop() + getPaddingBottom();
        int rowNum = Math.min(mMaxRows, mChildNumForRowLst.size());
        measuredHeight += mRowSpacing * (rowNum - 1);


        measuredWidth = widthMode == MeasureSpec.EXACTLY ? widthSize : measuredWidth;
        measuredHeight = heightMode == MeasureSpec.EXACTLY ? heightSize : measuredHeight;
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int x = paddingLeft;
        int y = paddingTop;

        int rowCount = mChildNumForRowLst.size();
        int childIndex = 0;
        for (int row = 0; row < rowCount; row++) {
            int childCount = mChildNumForRowLst.get(row);
            int rowHeight = mHeightForRowLst.get(row);
            for (int i = 0; i < childCount & childIndex < getChildCount(); ) {
                View child = getChildAt(childIndex++);
                if (child.getVisibility() == GONE) {
                    continue;
                } else {
                    i++;
                }

                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int childMarginLeft = lp.leftMargin;
                int childMarginRight = lp.rightMargin;
                int childMarginTop = lp.topMargin;

                child.layout(x + childMarginLeft,
                        y + childMarginTop,
                        x + childMarginLeft + child.getMeasuredWidth(),
                        y + childMarginTop + child.getMeasuredHeight());

                x += childMarginLeft + child.getMeasuredWidth() + childMarginRight + mChildSpacing;
            }
            x = paddingLeft;
            y += rowHeight + mRowSpacing;
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }
}
