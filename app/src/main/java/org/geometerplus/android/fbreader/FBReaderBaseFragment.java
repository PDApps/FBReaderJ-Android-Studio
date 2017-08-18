package org.geometerplus.android.fbreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.WindowManager;

import com.github.johnpersano.supertoasts.SuperActivityToast;

import org.geometerplus.android.fbreader.dict.DictionaryUtil;
import org.geometerplus.android.fbreader.util.FBReaderAdapter;
import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;

/**
 * Created by Andrew Churilo on 17.08.2017.
 */

public abstract class FBReaderBaseFragment extends Fragment implements FBReaderAdapter {
    public static final int REQUEST_PREFERENCES = 1;
    public static final int REQUEST_CANCEL_MENU = 2;
    public static final int REQUEST_DICTIONARY = 3;

    private volatile SuperActivityToast myToast;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(getActivity()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
            case REQUEST_DICTIONARY:
                DictionaryUtil.onActivityResult(null, resultCode, data);
                break;
        }
    }

    public ZLAndroidLibrary getZLibrary() {
        return ZLAndroidApplication.getInstance().library();
    }

    /* ++++++ SCREEN BRIGHTNESS ++++++ */
    protected void setScreenBrightnessAuto() {
        final WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
        attrs.screenBrightness = -1.0f;
        getActivity().getWindow().setAttributes(attrs);
    }

    public void setScreenBrightnessSystem(float level) {
        final WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
        attrs.screenBrightness = level;
        getActivity().getWindow().setAttributes(attrs);
    }

    public float getScreenBrightnessSystem() {
        final float level = getActivity().getWindow().getAttributes().screenBrightness;
        return level >= 0 ? level : .5f;
    }
    /* ------ SCREEN BRIGHTNESS ------ */

    /* ++++++ SUPER TOAST ++++++ */
    public boolean isToastShown() {
        final SuperActivityToast toast = myToast;
        return toast != null && toast.isShowing();
    }

    public void hideToast() {
        final SuperActivityToast toast = myToast;
        if (toast != null && toast.isShowing()) {
            myToast = null;
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    toast.dismiss();
                }
            });
        }
    }

    public void showToast(final SuperActivityToast toast) {
        hideToast();
        myToast = toast;
        // TODO: avoid this hack (accessing text style via option)
        final int dpi = getZLibrary().getDisplayDPI();
        final int defaultFontSize = dpi * 18 / 160;
        final int fontSize = new ZLIntegerOption("Style", "Base:fontSize", defaultFontSize).getValue();
        final int percent = new ZLIntegerRangeOption("Options", "ToastFontSizePercent", 25, 100, 90).getValue();
        final int dpFontSize = fontSize * 160 * percent / dpi / 100;
        toast.setTextSize(dpFontSize);
        toast.setButtonTextSize(dpFontSize * 7 / 8);

        final String fontFamily =
                new ZLStringOption("Style", "Base:fontFamily", "sans-serif").getValue();
        toast.setTypeface(AndroidFontUtil.systemTypeface(fontFamily, false, false));

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                toast.show();
            }
        });
    }
	/* ------ SUPER TOAST ------ */

    public abstract void hideDictionarySelection();
}
