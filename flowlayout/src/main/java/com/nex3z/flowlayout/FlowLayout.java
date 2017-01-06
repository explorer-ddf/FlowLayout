package com.nex3z.flowlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {
    private static final String LOG_TAG = FlowLayout.class.getSimpleName();

    public static final int SPACING_AUTO = -65536;
    public static final int SPACING_ALIGN = -65537;
    public static final int SPACING_UNDEFINED = -65538;

    private static final boolean DEFAULT_FLOW = true;
    private static final int DEFAULT_CHILD_SPACING = 0;
    private static final int DEFAULT_CHILD_SPACING_FOR_LAST_ROW = 0;
    private static final float DEFAULT_ROW_SPACING = 0;

    private boolean mFlow = DEFAULT_FLOW;
    private int mChildSpacing = DEFAULT_CHILD_SPACING;
    private int mChildSpacingForLastRow = DEFAULT_CHILD_SPACING_FOR_LAST_ROW;
    private float mRowSpacing = DEFAULT_ROW_SPACING;
    private float mAdjustedRowSpacing = DEFAULT_ROW_SPACING;

    private List<Float> mHorizontalSpacingForRow = new ArrayList<>();
    private List<Integer> mHeightForRow = new ArrayList<>();
    private List<Integer> mChildNumForRow = new ArrayList<>();

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FlowLayout, 0, 0);
        try {
            mFlow = a.getBoolean(R.styleable.FlowLayout_flow, DEFAULT_FLOW);
            try {
                mChildSpacing = a.getInt(R.styleable.FlowLayout_childSpacing, DEFAULT_CHILD_SPACING);
            } catch (NumberFormatException e) {
                mChildSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_childSpacing, (int)dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mChildSpacingForLastRow = a.getInt(R.styleable.FlowLayout_childSpacingForLastRow, SPACING_UNDEFINED);
            } catch (NumberFormatException e) {
                mChildSpacingForLastRow = a.getDimensionPixelSize(R.styleable.FlowLayout_childSpacingForLastRow, (int)dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mRowSpacing = a.getInt(R.styleable.FlowLayout_rowSpacing, 0);
            }  catch (NumberFormatException e) {
                mRowSpacing = a.getDimension(R.styleable.FlowLayout_rowSpacing, dpToPx(DEFAULT_ROW_SPACING));
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        mHorizontalSpacingForRow.clear();
        mChildNumForRow.clear();
        mHeightForRow.clear();
        int measuredHeight = 0, measuredWidth = 0, childCount = getChildCount();
        int rowWidth = 0, maxChildHeightInRow = 0, childNumInRow = 0;
        int rowSize = widthSize - getPaddingLeft() - getPaddingRight();
        float tmpSpacing = mChildSpacing == SPACING_AUTO ? 0 : mChildSpacing;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            LayoutParams childParams = child.getLayoutParams();
            int horizontalMargin = 0, verticalMargin = 0;
            if (childParams instanceof MarginLayoutParams) {
                measureChildWithMargins(child, widthMeasureSpec, 0,heightMeasureSpec, measuredHeight);
                MarginLayoutParams marginParams = (MarginLayoutParams) childParams;
                horizontalMargin = marginParams.leftMargin + marginParams.rightMargin;
                verticalMargin = marginParams.topMargin + marginParams.bottomMargin;
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }

            int childWidth = child.getMeasuredWidth() + horizontalMargin;
            int childHeight = child.getMeasuredHeight() + verticalMargin;
            if (mFlow && rowWidth + childWidth > rowSize) { // Need flow to next row
                // Save parameters for current row
                mHorizontalSpacingForRow.add(
                        getSpacingForRow(mChildSpacing, rowSize, rowWidth, childNumInRow));
                mChildNumForRow.add(childNumInRow);
                mHeightForRow.add(maxChildHeightInRow);
                measuredHeight += maxChildHeightInRow;
                measuredWidth = max(measuredWidth, rowWidth);

                // Place the button to next row
                childNumInRow = 1;
                rowWidth = childWidth;
                maxChildHeightInRow = childHeight;
            } else {
                childNumInRow++;
                rowWidth += childWidth + tmpSpacing;
                maxChildHeightInRow = max(maxChildHeightInRow, childHeight);
            }
        }

        // Measure remaining buttons in the last row
        if (mChildSpacingForLastRow == SPACING_ALIGN) {
            // For SPACING_ALIGN, use the same spacing from the row above if there is more than one
            // row.
            if (mHorizontalSpacingForRow.size() >= 1) {
                mHorizontalSpacingForRow.add(
                        mHorizontalSpacingForRow.get(mHorizontalSpacingForRow.size() - 1));
            } else {
                mHorizontalSpacingForRow.add(
                        getSpacingForRow(mChildSpacing, rowSize, rowWidth, childNumInRow));
            }
        } else if (mChildSpacingForLastRow != SPACING_UNDEFINED) {
            mHorizontalSpacingForRow.add(
                    getSpacingForRow(mChildSpacingForLastRow, rowSize, rowWidth, childNumInRow));
        } else {
            mHorizontalSpacingForRow.add(
                    getSpacingForRow(mChildSpacing, rowSize, rowWidth, childNumInRow));
        }

        mChildNumForRow.add(childNumInRow);
        mHeightForRow.add(maxChildHeightInRow);
        measuredHeight += maxChildHeightInRow;
        measuredWidth = max(measuredWidth, rowWidth);

        if (mChildSpacing == SPACING_AUTO) {
            measuredWidth = widthSize;
        } else {
            measuredWidth = min(measuredWidth + getPaddingLeft() + getPaddingRight(), widthSize);
        }

        measuredHeight += getPaddingTop() + getPaddingBottom();
        int rowNum = mHorizontalSpacingForRow.size();
        if (mRowSpacing == SPACING_AUTO) {
            mAdjustedRowSpacing = (heightSize - measuredHeight) / (rowNum - 1);
            measuredHeight = heightSize;
        } else {
            mAdjustedRowSpacing = mRowSpacing;
            measuredHeight = min(
                    (int)(measuredHeight + mAdjustedRowSpacing * (rowNum - 1)), heightSize);
        }

        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(measuredWidth, measuredHeight);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, measuredHeight);
        } else if (widthMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(measuredWidth, heightSize);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int x = paddingLeft;
        int y = paddingTop;

        int rowCount = mChildNumForRow.size(), childIdx = 0;
        for (int row = 0; row < rowCount; row++) {
            int buttonNum = mChildNumForRow.get(row);
            int rowHeight = mHeightForRow.get(row);
            float spacing = mHorizontalSpacingForRow.get(row);
            for (int i = 0; i < buttonNum; i++) {
                View child = getChildAt(childIdx++);
                if (child.getVisibility() == GONE) {
                    continue;
                }

                LayoutParams childParams = child.getLayoutParams();
                int marginLeft = 0, marginTop = 0, marginRight = 0;
                if (childParams instanceof MarginLayoutParams) {
                    MarginLayoutParams marginParams = (MarginLayoutParams) childParams;
                    marginLeft = marginParams.leftMargin;
                    marginRight = marginParams.rightMargin;
                    marginTop = marginParams.topMargin;
                }

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                child.layout(x + marginLeft, y + marginTop,
                        x + marginLeft + childWidth, y + marginTop + childHeight);
                x += childWidth + spacing + marginLeft + marginRight;
            }
            x = paddingLeft;
            y += rowHeight + mAdjustedRowSpacing;
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private int max(int a, int b) {
        return a > b ? a : b;
    }

    private int min(int a, int b) {
        return a < b ? a : b;
    }

    private float getSpacingForRow(int spacingAttribute, int rowSize, int usedSize, int buttonNum) {
        float spacing;
        if (spacingAttribute == SPACING_AUTO) {
            if (buttonNum > 1) {
                spacing = (rowSize - usedSize) / (buttonNum - 1);
            } else {
                spacing = 0;
            }
        } else {
            spacing = spacingAttribute;
        }
        return spacing;
    }

    private float dpToPx(float dp){
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}