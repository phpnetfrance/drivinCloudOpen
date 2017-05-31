package org.phpnet.openDrivinCloudAndroid.Component;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.phpnet.openDrivinCloudAndroid.Adapter.SyncsListAdapter;

/**
 * Created by clement on 22/08/16.
 */
public class SyncsListDivider extends RecyclerView.ItemDecoration {
    private static final String TAG = SyncsListDivider.class.getSimpleName();
    private Drawable mDivider;
    private int mOffsetTop = 0;
    private int mOffsetBottom = 0;

    /**
     * @param mDivider the drawable to put between items
     * @param offsetTop The offset on top of the list (can be null/0)
     * @param offsetBottom The offset on list bottom (can be null/0)
     */
    public SyncsListDivider(Drawable mDivider, @Nullable Integer offsetTop, @Nullable Integer offsetBottom) {
        this.mDivider = mDivider;
        if(offsetBottom != null) this.mOffsetBottom = offsetBottom.intValue();
        if(offsetTop != null) this.mOffsetTop = offsetTop.intValue();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        int dividerRight = parent.getWidth();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            int viewType = parent.getAdapter().getItemViewType(position);
            int nextViewType = parent.getAdapter().getItemViewType(position+1);
            //We don't want to draw dividers between header and body:
            if(viewType == SyncsListAdapter.BODY || (viewType == SyncsListAdapter.HEADER && nextViewType == SyncsListAdapter.HEADER)) {

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int dividerTop = child.getBottom() + params.bottomMargin;
                int dividerBottom = dividerTop + mDivider.getIntrinsicHeight();

                mDivider.setBounds(0, dividerTop, dividerRight, dividerBottom);
                mDivider.draw(c);
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);



        if (parent.getChildAdapterPosition(view) == 0) { //First child add offset
            outRect.top = mOffsetTop;
            return;
        }

        if(parent.getChildAdapterPosition(view) == state.getItemCount()-1){ //Last child add offset
            outRect.bottom = mOffsetBottom;
            return;
        }

        outRect.top = mDivider.getIntrinsicHeight();
    }
}
