package org.geometerplus.android.fbreader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.geometerplus.android.fbreader.api.ApiListener;
import org.geometerplus.android.fbreader.api.ApiServerImplementation;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.api.MenuNode;
import org.geometerplus.android.fbreader.api.PluginApi;
import org.geometerplus.android.fbreader.httpd.DataService;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.DeviceType;
import org.geometerplus.android.util.UIMessageUtil;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.options.CancelMenuHelper;
import org.geometerplus.fbreader.fbreader.options.ColorProfile;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.error.ErrorKeys;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;

/**
 * Created by Andrew Churilo on 17.08.2017.
 */

public abstract class FBReaderFragment extends FBReaderBaseFragment implements ZLApplicationWindow {
    public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
    public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;

    private FBReaderApp myFBReaderApp;
    protected volatile Book myBook;

    private RelativeLayout myRootView;
    private ZLAndroidWidget myMainView;

    private volatile boolean myShowStatusBarFlag;
    private String myMenuLanguage;

    final DataService.Connection DataConnection = new DataService.Connection();

    volatile boolean IsPaused = false;
    private volatile long myResumeTimestamp;
    volatile Runnable OnResumeAction = null;

    private Intent myCancelIntent = null;
    private Intent myOpenBookIntent = null;

    private static final String PLUGIN_ACTION_PREFIX = "___";
    private final List<PluginApi.ActionInfo> myPluginActions =
            new LinkedList<PluginApi.ActionInfo>();
    private final BroadcastReceiver myPluginInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ArrayList<PluginApi.ActionInfo> actions = getResultExtras(true).<PluginApi.ActionInfo>getParcelableArrayList(PluginApi.PluginInfo.KEY);
            if (actions != null) {
                synchronized (myPluginActions) {
                    int index = 0;
                    while (index < myPluginActions.size()) {
                        myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++);
                    }
                    myPluginActions.addAll(actions);
                    index = 0;
                    for (PluginApi.ActionInfo info : myPluginActions) {
                        myFBReaderApp.addAction(
                                PLUGIN_ACTION_PREFIX + index++,
                                new RunPluginAction(FBReaderFragment.this.getActivity(), myFBReaderApp, info.getId())
                        );
                    }
                }
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private synchronized void openBook(Intent intent, final Runnable action, boolean force) {
        if (!force && myBook != null) {
            return;
        }

        myBook = FBReaderIntents.getBookExtra(intent, myFBReaderApp.Collection);
        final Bookmark bookmark = FBReaderIntents.getBookmarkExtra(intent);
        if (myBook == null) {
            ZLFile zlFile = getZLFile();
            if (zlFile != null) {
                myBook = createBookForFile(zlFile);
            }
        }
        if (myBook != null) {
            ZLFile file = BookUtil.fileByBook(myBook);
            if (!file.exists()) {
                if (file.getPhysicalFile() != null) {
                    file = file.getPhysicalFile();
                }
                UIMessageUtil.showErrorMessage(getActivity(), "fileNotFound", file.getPath());
                myBook = null;
            } else {
                NotificationUtil.drop(getActivity(), myBook);
            }
        }
        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                myFBReaderApp.openBook(myBook, bookmark, action);
                AndroidFontUtil.clearFontCache();
            }
        });
    }

    protected abstract ZLFile getZLFile();

    protected Book createBookForFile(ZLFile file) {
        if (file == null) {
            return null;
        }
        Book book = myFBReaderApp.Collection.getBookByFile(file.getPath());
        if (book != null) {
            return book;
        }
        if (file.isArchive()) {
            for (ZLFile child : file.children()) {
                book = myFBReaderApp.Collection.getBookByFile(child.getPath());
                if (book != null) {
                    return book;
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().bindService(
                new Intent(getActivity(), DataService.class),
                DataConnection,
                DataService.BIND_AUTO_CREATE
        );

        final Config config = Config.Instance();
        config.runOnConnect(new Runnable() {
            public void run() {
                config.requestAllValuesForGroup("Options");
                config.requestAllValuesForGroup("Style");
                config.requestAllValuesForGroup("LookNFeel");
                config.requestAllValuesForGroup("Fonts");
                config.requestAllValuesForGroup("Colors");
                config.requestAllValuesForGroup("Files");
            }
        });

        final ZLAndroidLibrary zlibrary = getZLibrary();
        myShowStatusBarFlag = zlibrary.ShowStatusBarOption.getValue();


        View mainLayout = inflater.inflate(R.layout.main, null);
        myRootView = (RelativeLayout) mainLayout.findViewById(R.id.root_view);
        myMainView = (ZLAndroidWidget) mainLayout.findViewById(R.id.main_view);
        getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);

        myFBReaderApp = (FBReaderApp) FBReaderApp.Instance();
        if (myFBReaderApp == null) {
            myFBReaderApp = new FBReaderApp(Paths.systemInfo(getActivity()), new BookCollectionShadow());
        }
        getCollection().bindToService(getActivity(), null);
        myBook = null;

        myFBReaderApp.setWindow(this);
        myFBReaderApp.initWindow();

        myFBReaderApp.setExternalFileOpener(new ExternalFileOpener(getActivity(), this));

        getActivity().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                myShowStatusBarFlag ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        if (myFBReaderApp.getPopupById(TextSearchPopup.ID) == null) {
            new TextSearchPopup(myFBReaderApp);
        }
        if (myFBReaderApp.getPopupById(NavigationPopup.ID) == null) {
            new NavigationPopup(myFBReaderApp);
        }
        if (myFBReaderApp.getPopupById(SelectionPopup.ID) == null) {
            new SelectionPopup(myFBReaderApp);
        }

        Activity activity = getActivity();
        myFBReaderApp.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHOW_TOC, new ShowTOCAction(activity, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SEARCH, new SearchAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SHARE_BOOK, new ShareBookAction(activity, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(activity, myFBReaderApp, this));

        myFBReaderApp.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(activity, myFBReaderApp, this));
        myFBReaderApp.addAction(ActionCode.OPEN_VIDEO, new OpenVideoAction(activity, myFBReaderApp, this));
        myFBReaderApp.addAction(ActionCode.HIDE_TOAST, new HideToastAction(activity, myFBReaderApp, this));

        myFBReaderApp.addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.OPEN_START_SCREEN, new StartScreenAction(activity, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new SetScreenOrientationAction(activity, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new SetScreenOrientationAction(activity, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_SENSOR));
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetScreenOrientationAction(activity, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
        myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetScreenOrientationAction(activity, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));
        if (getZLibrary().supportsAllOrientations()) {
            myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new SetScreenOrientationAction(activity, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
            myFBReaderApp.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new SetScreenOrientationAction(activity, myFBReaderApp, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
        }
        myFBReaderApp.addAction(ActionCode.OPEN_WEB_HELP, new OpenWebHelpAction(activity, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.INSTALL_PLUGINS, new InstallPluginsAction(activity, myFBReaderApp));

        myFBReaderApp.addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(activity, myFBReaderApp, ColorProfile.DAY));
        myFBReaderApp.addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction(activity, myFBReaderApp, ColorProfile.NIGHT));

        final Intent intent = getActivity().getIntent();
        final String action = intent.getAction();

        myOpenBookIntent = intent;
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (FBReaderIntents.Action.CLOSE.equals(action)) {
                myCancelIntent = intent;
                myOpenBookIntent = null;
            } else if (FBReaderIntents.Action.PLUGIN_CRASH.equals(action)) {
                myFBReaderApp.ExternalBook = null;
                myOpenBookIntent = null;
                getCollection().bindToService(getActivity(), new Runnable() {
                    public void run() {
                        myFBReaderApp.openBook(null, null, null);
                    }
                });
            }
        }

        return mainLayout;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setStatusBarVisibility(true);
        setupMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        setStatusBarVisibility(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        setStatusBarVisibility(false);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        initPluginActions();

        final ZLAndroidLibrary zlibrary = getZLibrary();

        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                final boolean showStatusBar = zlibrary.ShowStatusBarOption.getValue();
                zlibrary.ShowStatusBarOption.saveSpecialValue();
                myFBReaderApp.ViewOptions.ColorProfileName.saveSpecialValue();
                SetScreenOrientationAction.setOrientation(getActivity(), zlibrary.getOrientationOption().getValue());
            }
        });

        ((PopupPanel) myFBReaderApp.getPopupById(TextSearchPopup.ID)).setPanelInfo(getActivity(), myRootView);
        ((NavigationPopup) myFBReaderApp.getPopupById(NavigationPopup.ID)).setPanelInfo(getActivity(), myRootView);
        ((PopupPanel) myFBReaderApp.getPopupById(SelectionPopup.ID)).setPanelInfo(getActivity(), myRootView);
    }

    private void initPluginActions() {
        synchronized (myPluginActions) {
            int index = 0;
            while (index < myPluginActions.size()) {
                myFBReaderApp.removeAction(PLUGIN_ACTION_PREFIX + index++);
            }
            myPluginActions.clear();
        }

        getActivity().sendOrderedBroadcast(
                new Intent(PluginApi.ACTION_REGISTER),
                null,
                myPluginInfoReceiver,
                null,
                RESULT_OK,
                null,
                null
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        Config.Instance().runOnConnect(new Runnable() {
            public void run() {
                setScreenBrightnessAuto();
                if (getZLibrary().DisableButtonLightsOption.getValue()) {
                    setButtonLight(false);
                }

                getCollection().bindToService(getActivity(), new Runnable() {
                    public void run() {
                        final BookModel model = myFBReaderApp.Model;
                        if (model == null || model.Book == null) {
                            return;
                        }
                        onPreferencesUpdate(myFBReaderApp.Collection.getBookById(model.Book.getId()));
                    }
                });
            }
        });

        getActivity().registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IsPaused = false;
        myResumeTimestamp = System.currentTimeMillis();
        if (OnResumeAction != null) {
            final Runnable action = OnResumeAction;
            OnResumeAction = null;
            action.run();
        }

        SetScreenOrientationAction.setOrientation(getActivity(), getZLibrary().getOrientationOption().getValue());
        if (myCancelIntent != null) {
            final Intent intent = myCancelIntent;
            myCancelIntent = null;
            getCollection().bindToService(getActivity(), new Runnable() {
                public void run() {
                    runCancelAction(intent);
                }
            });
            return;
        } else if (myOpenBookIntent != null) {
            final Intent intent = myOpenBookIntent;
            myOpenBookIntent = null;
            getCollection().bindToService(getActivity(), new Runnable() {
                public void run() {
                    openBook(intent, null, true);
                }
            });
        } else if (myFBReaderApp.Model == null && myFBReaderApp.ExternalBook != null) {
            getCollection().bindToService(getActivity(), new Runnable() {
                public void run() {
                    myFBReaderApp.openBook(myFBReaderApp.ExternalBook, null, null);
                }
            });
        }

        PopupPanel.restoreVisibilities(myFBReaderApp);
        ApiServerImplementation.sendEvent(getActivity(), ApiListener.EVENT_READ_MODE_OPENED);
    }

    @Override
    public void onPause() {
        IsPaused = true;
        try {
            getActivity().unregisterReceiver(myBatteryInfoReceiver);
        } catch (IllegalArgumentException e) {
            // do nothing, this exception means that myBatteryInfoReceiver was not registered
        }

        myFBReaderApp.stopTimer();
        if (getZLibrary().DisableButtonLightsOption.getValue()) {
            setButtonLight(true);
        }
        myFBReaderApp.onWindowClosing();

        super.onPause();
    }

    @Override
    public void onStop() {
        ApiServerImplementation.sendEvent(getActivity(), ApiListener.EVENT_READ_MODE_CLOSED);
        PopupPanel.removeAllWindows(myFBReaderApp, getActivity());
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        getCollection().unbind();
        getActivity().unbindService(DataConnection);
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        myFBReaderApp.onWindowClosing();
        super.onLowMemory();
    }

    private void onPreferencesUpdate(Book book) {
        AndroidFontUtil.clearFontCache();
        myFBReaderApp.onBookUpdated(book);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
            case REQUEST_PREFERENCES:
                if (resultCode != RESULT_DO_NOTHING && data != null) {
                    final Book book = FBReaderIntents.getBookExtra(data, myFBReaderApp.Collection);
                    if (book != null) {
                        getCollection().bindToService(getActivity(), new Runnable() {
                            public void run() {
                                onPreferencesUpdate(book);
                            }
                        });
                    }
                }
                break;
            case REQUEST_CANCEL_MENU:
                runCancelAction(data);
                break;
        }
    }

    private void runCancelAction(Intent intent) {
        final CancelMenuHelper.ActionType type;
        try {
            type = CancelMenuHelper.ActionType.valueOf(
                    intent.getStringExtra(FBReaderIntents.Key.TYPE)
            );
        } catch (Exception e) {
            // invalid (or null) type value
            return;
        }
        Bookmark bookmark = null;
        if (type == CancelMenuHelper.ActionType.returnTo) {
            bookmark = FBReaderIntents.getBookmarkExtra(intent);
            if (bookmark == null) {
                return;
            }
        }
        myFBReaderApp.runCancelAction(type, bookmark);
    }

    private Menu addSubmenu(Menu menu, String id) {
        return menu.addSubMenu(ZLResource.resource("menu").getResource(id).getValue());
    }

    private void addMenuItem(Menu menu, String actionId, Integer iconId, String name) {
        if (name == null) {
            name = ZLResource.resource("menu").getResource(actionId).getValue();
        }
        final MenuItem menuItem = menu.add(name);
        if (iconId != null) {
            menuItem.setIcon(iconId);
        }
        menuItem.setOnMenuItemClickListener(myMenuListener);
        myMenuItemMap.put(menuItem, actionId);
    }

    private void addMenuItem(Menu menu, String actionId, String name) {
        addMenuItem(menu, actionId, null, name);
    }

    private void addMenuItem(Menu menu, String actionId, int iconId) {
        addMenuItem(menu, actionId, iconId, null);
    }

    private void addMenuItem(Menu menu, String actionId) {
        addMenuItem(menu, actionId, null, null);
    }

    private void fillMenu(Menu menu, List<MenuNode> nodes) {
        for (MenuNode n : nodes) {
            if (n instanceof MenuNode.Item) {
                final Integer iconId = ((MenuNode.Item) n).IconId;
                if (iconId != null) {
                    addMenuItem(menu, n.Code, iconId);
                } else {
                    addMenuItem(menu, n.Code);
                }
            } else /* if (n instanceof MenuNode.Submenu) */ {
                final Menu submenu = addSubmenu(menu, n.Code);
                fillMenu(submenu, ((MenuNode.Submenu) n).Children);
            }
        }
    }

    private void setupMenu(Menu menu) {
        final String menuLanguage = ZLResource.getLanguageOption().getValue();
        if (menuLanguage.equals(myMenuLanguage)) {
            return;
        }
        myMenuLanguage = menuLanguage;

        menu.clear();
        fillMenu(menu, MenuData.topLevelNodes());
        synchronized (myPluginActions) {
            int index = 0;
            for (PluginApi.ActionInfo info : myPluginActions) {
                if (info instanceof PluginApi.MenuActionInfo) {
                    addMenuItem(
                            menu,
                            PLUGIN_ACTION_PREFIX + index++,
                            ((PluginApi.MenuActionInfo) info).MenuItemName
                    );
                }
            }
        }

        refresh();
    }

    @Override
    public void onPluginNotFound(final Book book) {
        final BookCollectionShadow collection = getCollection();
        collection.bindToService(getActivity(), new Runnable() {
            public void run() {
                final Book recent = collection.getRecentBook(0);
                if (recent != null && !collection.sameBook(recent, book)) {
                    myFBReaderApp.openBook(recent, null, null);
                } else {
                    myFBReaderApp.openHelpBook();
                }
            }
        });
    }

    private void setStatusBarVisibility(boolean visible) {
        final ZLAndroidLibrary zlibrary = getZLibrary();
        if (DeviceType.Instance() != DeviceType.KINDLE_FIRE_1ST_GENERATION && !myShowStatusBarFlag) {
            if (visible) {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            } else {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        }
    }

    @Override
    public boolean isPaused() {
        return IsPaused;
    }

    @Override
    public void setOnResumeAction(Runnable onResumeAction) {
        OnResumeAction = onResumeAction;
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        return (myMainView != null && myMainView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        return (myMainView != null && myMainView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
//    }

    private void setButtonLight(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            setButtonLightInternal(enabled);
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void setButtonLightInternal(boolean enabled) {
        final WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
        attrs.buttonBrightness = enabled ? -1.0f : 0.0f;
        getActivity().getWindow().setAttributes(attrs);
    }

    private BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final int level = intent.getIntExtra("level", 100);
            final ZLAndroidApplication application = (ZLAndroidApplication) getActivity().getApplication();
            setBatteryLevel(level);
        }
    };

    private BookCollectionShadow getCollection() {
        return (BookCollectionShadow) myFBReaderApp.Collection;
    }

    // methods from ZLApplicationWindow interface
    @Override
    public void showErrorMessage(String key) {
        UIMessageUtil.showErrorMessage(getActivity(), key);
    }

    @Override
    public void showErrorMessage(String key, String parameter) {
        UIMessageUtil.showErrorMessage(getActivity(), key, parameter);
    }

    @Override
    public FBReaderApp.SynchronousExecutor createExecutor(String key) {
        return UIUtil.createExecutor(getActivity(), key);
    }

    private int myBatteryLevel;

    @Override
    public int getBatteryLevel() {
        return myBatteryLevel;
    }

    private void setBatteryLevel(int percent) {
        myBatteryLevel = percent;
    }

    @Override
    public void close() {
        getActivity().finish();
    }

    @Override
    public ZLViewWidget getViewWidget() {
        return myMainView;
    }

    private final HashMap<MenuItem, String> myMenuItemMap = new HashMap<MenuItem, String>();

    private final MenuItem.OnMenuItemClickListener myMenuListener =
            new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    myFBReaderApp.runAction(myMenuItemMap.get(item));
                    return true;
                }
            };

    @Override
    public void refresh() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                for (Map.Entry<MenuItem, String> entry : myMenuItemMap.entrySet()) {
                    final String actionId = entry.getValue();
                    final MenuItem menuItem = entry.getKey();
                    menuItem.setVisible(myFBReaderApp.isActionVisible(actionId) && myFBReaderApp.isActionEnabled(actionId));
                    switch (myFBReaderApp.isActionChecked(actionId)) {
                        case TRUE:
                            menuItem.setCheckable(true);
                            menuItem.setChecked(true);
                            break;
                        case FALSE:
                            menuItem.setCheckable(true);
                            menuItem.setChecked(false);
                            break;
                        case UNDEFINED:
                            menuItem.setCheckable(false);
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();

        final Intent intent = new Intent(
                FBReaderIntents.Action.ERROR,
                new Uri.Builder().scheme(exception.getClass().getSimpleName()).build()
        );
        intent.setPackage(FBReaderIntents.DEFAULT_PACKAGE);
        intent.putExtra(ErrorKeys.MESSAGE, exception.getMessage());
        final StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString());
        /*
		if (exception instanceof BookReadingException) {
			final ZLFile file = ((BookReadingException)exception).File;
			if (file != null) {
				intent.putExtra("file", file.getPath());
			}
		}
		*/
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // ignore
            e.printStackTrace();
        }
    }

    @Override
    public void setWindowTitle(final String title) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                getActivity().setTitle(title);
            }
        });
    }

    @Override
    public void outlineRegion(ZLTextRegion.Soul soul) {
        myFBReaderApp.getTextView().outlineRegion(soul);
        myFBReaderApp.getViewWidget().repaint();
    }

    public void hideOutline() {
        myFBReaderApp.getTextView().hideOutline();
        myFBReaderApp.getViewWidget().repaint();
    }

    @Override
    public DataService.Connection getDataConnection() {
        return DataConnection;
    }
}
