package org.geometerplus.android.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.WindowManager;

public class ImmersiveProgressDialog extends ProgressDialog {
    private Activity activity;

    public ImmersiveProgressDialog(Context context) {
        super(context);
        if (context instanceof Activity) {
            activity = (Activity) context;
        }
    }

    public ImmersiveProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    public void show() {
        if (activity != null) {
            // Set the dialog to not focusable.
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            // Show the dialog
            super.show();

            // Set the dialog to immersive
            getWindow().getDecorView().setSystemUiVisibility(
                    activity.getWindow().getDecorView().getSystemUiVisibility());

            // Set the dialog to focusable again.
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        } else {
            super.show();
        }
    }
}
