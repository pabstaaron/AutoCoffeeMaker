package com.example.ndonaldson.beanster;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by ndonaldson on 6/7/18.
 */

public class VerticalSpace extends RecyclerView.ItemDecoration {

    private final int verticalSpaceHeight;

    public VerticalSpace(int verticalSpaceHeight) {
        this.verticalSpaceHeight = verticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.bottom = verticalSpaceHeight;
    }
}
