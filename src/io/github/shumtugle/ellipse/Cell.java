package io.github.shumtugle.ellipse;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A tile with a name under it: how an application is shown wherever it
 * is shown with its name, in the drawer and in an open folder alike.
 */
final class Cell {

    private Cell() {
    }

    static LinearLayout make(Context context, int tile, Tile.Look look) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(0, Round.dp(4f), 0, Round.dp(4f));
        cell.setBackground(Round.touch(null, Tone.of(Tone.ON_SURFACE), Round.L));
        cell.setStateListAnimator(Give.tile());
        ImageView face = new ImageView(context);
        face.setScaleType(ImageView.ScaleType.FIT_CENTER);
        face.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        cell.addView(face, new LinearLayout.LayoutParams(tile, Tile.height(tile, look)));
        TextView name = Letter.set(new TextView(context), Letter.BODY_M);
        name.setTextColor(Tone.of(Tone.ON_SURFACE));
        name.setGravity(Gravity.CENTER_HORIZONTAL);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        under.topMargin = Round.dp(8f);
        under.leftMargin = Round.dp(4f);
        under.rightMargin = Round.dp(4f);
        cell.addView(name, under);
        return cell;
    }

    static void dress(View made, App app, Icons icons, int tile) {
        LinearLayout cell = (LinearLayout) made;
        ImageView face = (ImageView) cell.getChildAt(0);
        TextView name = (TextView) cell.getChildAt(1);
        icons.put(face, app, tile);
        name.setText(app.label);
        cell.setContentDescription(app.label);
    }

    /** The tile of a cell, for whoever needs its picture. */
    static ImageView face(View cell) {
        return (ImageView) ((LinearLayout) cell).getChildAt(0);
    }

    static int width(View cell) {
        return ((LinearLayout) cell).getChildAt(0).getLayoutParams().width;
    }
}
