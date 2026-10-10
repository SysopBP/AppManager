// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later

package io.github.muntashirakon.AppManager.main;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.github.muntashirakon.AppManager.BaseActivity;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.apk.behavior.FreezeUnfreeze;
import io.github.muntashirakon.AppManager.apk.dexopt.DexOptDialog;
import io.github.muntashirakon.AppManager.apk.list.ListExporter;
import io.github.muntashirakon.AppManager.backup.dialog.BackupRestoreDialogFragment;
import io.github.muntashirakon.AppManager.batchops.BatchOpsManager;
import io.github.muntashirakon.AppManager.batchops.BatchOpsService;
import io.github.muntashirakon.AppManager.batchops.BatchQueueItem;
import io.github.muntashirakon.AppManager.batchops.struct.BatchFreezeOptions;
import io.github.muntashirakon.AppManager.batchops.struct.BatchNetPolicyOptions;
import io.github.muntashirakon.AppManager.batchops.struct.IBatchOpOptions;
import io.github.muntashirakon.AppManager.changelog.Changelog;
import io.github.muntashirakon.AppManager.changelog.ChangelogParser;
import io.github.muntashirakon.AppManager.changelog.ChangelogRecyclerAdapter;
import io.github.muntashirakon.AppManager.compat.NetworkPolicyManagerCompat;
import io.github.muntashirakon.AppManager.debloat.DebloaterActivity;
import io.github.muntashirakon.AppManager.filters.FinderActivity;
import io.github.muntashirakon.AppManager.misc.AdvancedSearchView;
import io.github.muntashirakon.AppManager.misc.HelpActivity;
import io.github.muntashirakon.AppManager.misc.LabsActivity;
import io.github.muntashirakon.AppManager.misc.SearchViewDebouncer;
import io.github.muntashirakon.AppManager.oneclickops.OneClickOpsActivity;
import io.github.muntashirakon.AppManager.profiles.AddToProfileDialogFragment;
import io.github.muntashirakon.AppManager.profiles.ProfilesActivity;
import io.github.muntashirakon.AppManager.rules.RulesTypeSelectionDialogFragment;
import io.github.muntashirakon.AppManager.runningapps.RunningAppsActivity;
import io.github.muntashirakon.AppManager.self.life.FundingCampaignChecker;
import io.github.muntashirakon.AppManager.settings.FeatureController;
import io.github.muntashirakon.AppManager.settings.Prefs;
import io.github.muntashirakon.AppManager.settings.Ops;
import io.github.muntashirakon.AppManager.settings.SettingsActivity;
import io.github.muntashirakon.AppManager.usage.AppUsageActivity;
import io.github.muntashirakon.AppManager.users.Users;
import io.github.muntashirakon.AppManager.utils.AppPref;
import io.github.muntashirakon.AppManager.utils.DateUtils;
import io.github.muntashirakon.AppManager.utils.StoragePermission;
import io.github.muntashirakon.AppManager.utils.UIUtils;
import io.github.muntashirakon.dialog.AlertDialogBuilder;
import io.github.muntashirakon.dialog.ScrollableDialogBuilder;
import io.github.muntashirakon.dialog.SearchableFlagsDialogBuilder;
import io.github.muntashirakon.dialog.SearchableSingleChoiceDialogBuilder;
import io.github.muntashirakon.io.Paths;
import io.github.muntashirakon.multiselection.MultiSelectionActionsView;
import io.github.muntashirakon.util.UiUtils;
import io.github.muntashirakon.widget.MultiSelectionView;
import io.github.muntashirakon.widget.SwipeRefreshLayout;

public class MainActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener,
        MultiSelectionActionsView.OnItemSelectedListener,
        MultiSelectionView.OnSelectionModeChangeListener {
    private static final String PACKAGE_NAME_APK_UPDATER = "com.apkupdater";
    private static final String ACTIVITY_NAME_APK_UPDATER = "com.apkupdater.activity.MainActivity";

    private static boolean SHOW_DISCLAIMER = true;

    MainViewModel viewModel;

    private MainRecyclerAdapter mAdapter;
    private AdvancedSearchView mSearchView;
    private SearchViewDebouncer mSearchDebouncer;
    private LinearProgressIndicator mProgressIndicator;
    private SwipeRefreshLayout mSwipeRefresh;
    private MultiSelectionView mMultiSelectionView;
    MainBatchOpsHandler mBatchOpsHandler;
    private MenuItem mAppUsageMenu;

    private final StoragePermission mStoragePermission = StoragePermission.init(this);

    private final ActivityResultLauncher<String> mBatchExportRules = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/tab-separated-values"),
            uri -> {
                if (uri == null) {
                    // Back button pressed.
                    return;
                }
                if (viewModel == null) {
                    // Invalid state
                    return;
                }
                RulesTypeSelectionDialogFragment dialogFragment = new RulesTypeSelectionDialogFragment();
                Bundle args = new Bundle();
                args.putInt(RulesTypeSelectionDialogFragment.ARG_MODE, RulesTypeSelectionDialogFragment.MODE_EXPORT);
                args.putParcelable(RulesTypeSelectionDialogFragment.ARG_URI, uri);
                args.putStringArrayList(RulesTypeSelectionDialogFragment.ARG_PKG, new ArrayList<>(viewModel.getSelectedPackages().keySet()));
                args.putIntArray(RulesTypeSelectionDialogFragment.ARG_USERS, Users.getUsersIds());
                dialogFragment.setArguments(args);
                dialogFragment.show(getSupportFragmentManager(), RulesTypeSelectionDialogFragment.TAG);
            });

    private final ActivityResultLauncher<String> mExportAppListCsv = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri == null) {
                    // Back button pressed.
                    return;
                }
                mProgressIndicator.show();
                viewModel.saveExportedAppList(ListExporter.EXPORT_TYPE_CSV, Paths.get(uri));
            });
    private final ActivityResultLauncher<String> mExportAppListJson = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri == null) {
                    // Back button pressed.
                    return;
                }
                mProgressIndicator.show();
                viewModel.saveExportedAppList(ListExporter.EXPORT_TYPE_JSON, Paths.get(uri));
            });
    private final ActivityResultLauncher<String> mExportAppListXml = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/xml"),
            uri -> {
                if (uri == null) {
                    // Back button pressed.
                    return;
                }
                mProgressIndicator.show();
                viewModel.saveExportedAppList(ListExporter.EXPORT_TYPE_XML, Paths.get(uri));
            });
    private final ActivityResultLauncher<String> mExportAppListMarkdown = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/markdown"),
            uri -> {
                if (uri == null) {
                    // Back button pressed.
                    return;
                }
                mProgressIndicator.show();
                viewModel.saveExportedAppList(ListExporter.EXPORT_TYPE_MARKDOWN, Paths.get(uri));
            });

    private final BroadcastReceiver mBatchOpsBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showProgressIndicator(false);
        }
    };

    private final OnBackPressedCallback mOnBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mAdapter != null && mMultiSelectionView != null && mAdapter.isInSelectionMode()) {
                mMultiSelectionView.cancel();
                return;
            }
            setEnabled(false);
            getOnBackPressedDispatcher().onBackPressed();
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onAuthenticated(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        getOnBackPressedDispatcher().addCallback(this, mOnBackPressedCallback);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
            mSearchView = new AdvancedSearchView(actionBar.getThemedContext());
            mSearchView.setId(R.id.action_search);
            // Set layout params
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            actionBar.setCustomView(mSearchView, layoutParams);
            mSearchView.setIconifiedByDefault(false);
            mSearchDebouncer = new SearchViewDebouncer(SearchViewDebouncer.DELAY_STANDARD);
            mSearchDebouncer.bindAdvanced(mSearchView, (query, type) -> {
                if (viewModel != null) viewModel.setSearchQuery(query, type);
            });
            // Check for market://search/?q=<query>
            Uri marketUri = getIntent().getData();
            if (marketUri != null && "market".equals(marketUri.getScheme()) && "search".equals(marketUri.getHost())) {
                String query = marketUri.getQueryParameter("q");
                if (query != null) {
                    mSearchView.setQuery(query, true);
                }
            }
        }

        mProgressIndicator = findViewById(R.id.progress_linear);
        mProgressIndicator.setVisibilityAfterHide(View.GONE);
        RecyclerView recyclerView = findViewById(R.id.item_list);
        recyclerView.requestFocus(); // Initially (the view isn't actually focusable)
        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        mSwipeRefresh.setOnRefreshListener(this);

        mAdapter = new MainRecyclerAdapter(MainActivity.this);
        mAdapter.setHasStableIds(true);
        recyclerView.setLayoutManager(UIUtils.getGridLayoutAt450Dp(this));
        recyclerView.setAdapter(mAdapter);
        // AppSphere floating bar: scroll down to hide, scroll up to reveal.
        final View floatingBar = findViewById(R.id.appsphere_floating_bar);
        final android.content.SharedPreferences appearance = getSharedPreferences(
                "appsphere_appearance", MODE_PRIVATE);
        if (appearance.getBoolean("haptics", true)) {
            for (int id : new int[]{R.id.appsphere_nav_home, R.id.appsphere_nav_apps,
                    R.id.appsphere_nav_running, R.id.appsphere_nav_installer, R.id.appsphere_nav_settings}) {
                findViewById(id).setHapticFeedbackEnabled(true);
            }
        }
        final View dashboard = findViewById(R.id.appsphere_dashboard);
        final View appList = findViewById(R.id.swipe_refresh);
        findViewById(R.id.appsphere_nav_home).setOnClickListener(v -> {
            dashboard.setVisibility(View.VISIBLE);
            appList.setVisibility(View.GONE);
            updateAppSphereBackendStatus();
            updateAppSphereSystemActivity();
            if (appearance.getBoolean("haptics", true)) v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (appearance.getBoolean("animations", true)) floatingBar.animate().translationY(0f).alpha(1f).setDuration(180).start();
            else { floatingBar.setTranslationY(0f); floatingBar.setAlpha(1f); }
        });
        findViewById(R.id.appsphere_nav_apps).setOnClickListener(v -> {
            dashboard.setVisibility(View.GONE);
            appList.setVisibility(View.VISIBLE);
            recyclerView.smoothScrollToPosition(0);
            floatingBar.animate().translationY(0f).alpha(1f).setDuration(180).start();
        });
        findViewById(R.id.appsphere_dashboard_apps).setOnClickListener(v ->
                findViewById(R.id.appsphere_nav_apps).performClick());
        findViewById(R.id.appsphere_dashboard_filters).setOnClickListener(v -> {
            findViewById(R.id.appsphere_nav_apps).performClick();
            MainListOptions options = new MainListOptions();
            options.setListOptionActions(viewModel);
            options.show(getSupportFragmentManager(), MainListOptions.TAG);
        });
        findViewById(R.id.appsphere_dashboard_running).setOnClickListener(v ->
                startActivity(new Intent(this, RunningAppsActivity.class)));
        findViewById(R.id.appsphere_backend_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.appsphere_backend_refresh).setOnClickListener(v ->
                updateAppSphereBackendStatus());
        findViewById(R.id.appsphere_activity_refresh).setOnClickListener(v ->
                updateAppSphereSystemActivity());
        findViewById(R.id.appsphere_command_search).setOnClickListener(v -> {
            findViewById(R.id.appsphere_nav_apps).performClick();
            if (mSearchView != null) {
                mSearchView.requestFocus();
                UiUtils.showKeyboard(mSearchView);
            }
        });
        findViewById(R.id.appsphere_installer_control).setOnClickListener(v ->
                showAppSphereInstallerControl());
        findViewById(R.id.appsphere_command_tools).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        // Long-press dock shortcuts reuse existing, working AppSphere screens.
        findViewById(R.id.appsphere_nav_apps).setOnLongClickListener(v -> {
            findViewById(R.id.appsphere_dashboard_filters).performClick();
            return true;
        });
        findViewById(R.id.appsphere_nav_home).setOnLongClickListener(v -> {
            findViewById(R.id.appsphere_nav_home).performClick();
            updateAppSphereBackendStatus();
            updateAppSphereSystemActivity();
            return true;
        });
        findViewById(R.id.appsphere_nav_installer).setOnClickListener(v ->
                showAppSphereInstallerControl());
        findViewById(R.id.appsphere_nav_installer).setOnLongClickListener(v -> {
            showAppSphereInstallerControl();
            return true;
        });
        findViewById(R.id.appsphere_nav_running).setOnClickListener(v ->
                startActivity(new Intent(this, RunningAppsActivity.class)));
        findViewById(R.id.appsphere_nav_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean hidden;
            private int accumulatedScroll;
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy == 0) return;
                // Avoid flicker from tiny scroll movements and touch jitter.
                if ((dy > 0 && accumulatedScroll < 0) || (dy < 0 && accumulatedScroll > 0)) {
                    accumulatedScroll = 0;
                }
                accumulatedScroll += dy;
                if (Math.abs(accumulatedScroll) < 18) return;
                boolean shouldHide = accumulatedScroll > 0 && rv.canScrollVertically(1);
                accumulatedScroll = 0;
                if (shouldHide == hidden) return;
                hidden = shouldHide;
                floatingBar.animate().cancel();
                floatingBar.animate()
                        .translationY(hidden ? floatingBar.getHeight() + floatingBar.getBottom() - rv.getBottom() + 24f * getResources().getDisplayMetrics().density : 0f)
                        .alpha(hidden ? 0f : 1f)
                        .setDuration(220).start();
            }
        });

        mMultiSelectionView = findViewById(R.id.selection_view);
        mMultiSelectionView.setOnItemSelectedListener(this);
        mMultiSelectionView.setOnSelectionModeChangeListener(this);
        mMultiSelectionView.setAdapter(mAdapter);
        mMultiSelectionView.updateCounter(true);
        mBatchOpsHandler = new MainBatchOpsHandler(mMultiSelectionView, viewModel);
        mMultiSelectionView.setOnSelectionChangeListener(mBatchOpsHandler);

        if (SHOW_DISCLAIMER && AppPref.getBoolean(AppPref.PrefKey.PREF_SHOW_DISCLAIMER_BOOL)) {
            // Disclaimer will only be shown the first time it is loaded.
            SHOW_DISCLAIMER = false;
            View view = View.inflate(this, R.layout.dialog_disclaimer, null);
            new MaterialAlertDialogBuilder(this)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.disclaimer_agree, (dialog, which) -> {
                        if (((MaterialCheckBox) view.findViewById(R.id.agree_forever)).isChecked()) {
                            AppPref.set(AppPref.PrefKey.PREF_SHOW_DISCLAIMER_BOOL, false);
                        }
                        displayChangelogIfRequired();
                    })
                    .setNegativeButton(R.string.disclaimer_exit, (dialog, which) -> finishAndRemoveTask())
                    .show();
        } else {
            displayChangelogIfRequired();
        }

        // Set observer
        viewModel.getApplicationItems().observe(this, applicationItems -> {
            if (mAdapter != null) {
                mAdapter.setDefaultList(applicationItems);
            }
            showProgressIndicator(false);
        });
        viewModel.getOperationStatus().observe(this, status -> {
            mProgressIndicator.hide();
            if (status) {
                UIUtils.displayShortToast(R.string.done);
            } else {
                UIUtils.displayLongToast(R.string.failed);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_actions, menu);
        mAppUsageMenu = menu.findItem(R.id.action_app_usage);
        MenuItem apkUpdaterMenu = menu.findItem(R.id.action_apk_updater);
        try {
            if (!getPackageManager().getApplicationInfo(PACKAGE_NAME_APK_UPDATER, 0).enabled)
                throw new PackageManager.NameNotFoundException();
            apkUpdaterMenu.setVisible(true);
        } catch (PackageManager.NameNotFoundException e) {
            apkUpdaterMenu.setVisible(false);
        }
        MenuItem finderMenu = menu.findItem(R.id.action_finder);
        finderMenu.setVisible(BuildConfig.DEBUG);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mAppUsageMenu.setVisible(FeatureController.isUsageAccessEnabled());
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_instructions) {
            Intent helpIntent = new Intent(this, HelpActivity.class);
            helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(helpIntent);
        } else if (id == R.id.action_list_options) {
            MainListOptions listOptions = new MainListOptions();
            listOptions.setListOptionActions(viewModel);
            listOptions.show(getSupportFragmentManager(), MainListOptions.TAG);
        } else if (id == R.id.action_refresh) {
            if (viewModel != null) {
                showProgressIndicator(true);
                viewModel.loadApplicationItems();
            }
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = SettingsActivity.getSettingsIntent(this);
            startActivity(settingsIntent);
        } else if (id == R.id.action_app_usage) {
            Intent usageIntent = new Intent(this, AppUsageActivity.class);
            startActivity(usageIntent);
        } else if (id == R.id.action_one_click_ops) {
            Intent onClickOpsIntent = new Intent(this, OneClickOpsActivity.class);
            startActivity(onClickOpsIntent);
        } else if (id == R.id.action_finder) {
            Intent intent = new Intent(this, FinderActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_apk_updater) {
            try {
                if (!getPackageManager().getApplicationInfo(PACKAGE_NAME_APK_UPDATER, 0).enabled)
                    throw new PackageManager.NameNotFoundException();
                Intent intent = new Intent();
                intent.setClassName(PACKAGE_NAME_APK_UPDATER, ACTIVITY_NAME_APK_UPDATER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
        } else if (id == R.id.action_running_apps) {
            Intent runningAppsIntent = new Intent(this, RunningAppsActivity.class);
            startActivity(runningAppsIntent);
        } else if (id == R.id.action_profiles) {
            Intent profilesIntent = new Intent(this, ProfilesActivity.class);
            startActivity(profilesIntent);
        } else if (id == R.id.action_labs) {
            Intent intent = new Intent(getApplicationContext(), LabsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (id == R.id.action_debloater) {
            Intent intent = new Intent(getApplicationContext(), DebloaterActivity.class);
            startActivity(intent);
        } else return super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public void onSelectionModeEnabled() {
        mOnBackPressedCallback.setEnabled(true);
    }

    @Override
    public void onSelectionModeDisabled() {
        mOnBackPressedCallback.setEnabled(false);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_backup) {
            if (viewModel != null) {
                BackupRestoreDialogFragment fragment = BackupRestoreDialogFragment.getInstance(viewModel.getSelectedPackagesWithUsers());
                fragment.setOnActionBeginListener(mode -> showProgressIndicator(true));
                fragment.setOnActionCompleteListener((mode, failedPackages) -> showProgressIndicator(false));
                fragment.show(getSupportFragmentManager(), BackupRestoreDialogFragment.TAG);
            }
        } else if (id == R.id.action_save_apk) {
            mStoragePermission.request(granted -> {
                if (granted) handleBatchOp(BatchOpsManager.OP_BACKUP_APK);
            });
        } else if (id == R.id.action_block_unblock_trackers) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.block_unblock_trackers)
                    .setMessage(R.string.choose_what_to_do)
                    .setPositiveButton(R.string.block, (dialog, which) ->
                            handleBatchOp(BatchOpsManager.OP_BLOCK_TRACKERS))
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.unblock, (dialog, which) ->
                            handleBatchOp(BatchOpsManager.OP_UNBLOCK_TRACKERS))
                    .show();
        } else if (id == R.id.action_clear_data_cache) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.clear)
                    .setMessage(R.string.choose_what_to_do)
                    .setPositiveButton(R.string.clear_cache, (dialog, which) ->
                            handleBatchOp(BatchOpsManager.OP_CLEAR_CACHE))
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.clear_data, (dialog, which) ->
                            handleBatchOp(BatchOpsManager.OP_CLEAR_DATA))
                    .show();
        } else if (id == R.id.action_freeze_unfreeze) {
            showFreezeUnfreezeDialog(Prefs.Blocking.getDefaultFreezingMethod());
        } else if (id == R.id.action_disable_background) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.disable_background_run_description)
                    .setPositiveButton(R.string.yes, (dialog, which) ->
                            handleBatchOp(BatchOpsManager.OP_DISABLE_BACKGROUND))
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else if (id == R.id.action_net_policy) {
            ArrayMap<Integer, String> netPolicyMap = NetworkPolicyManagerCompat.getAllReadablePolicies(this);
            Integer[] polices = new Integer[netPolicyMap.size()];
            String[] policyStrings = new String[netPolicyMap.size()];
            Collection<ApplicationItem> applicationItems = viewModel.getSelectedPackages().values();
            Iterator<ApplicationItem> it = applicationItems.iterator();
            int selectedPolicies = applicationItems.size() == 1 && it.hasNext() ?
                    NetworkPolicyManagerCompat.getUidPolicy(it.next().uid) : 0;
            for (int i = 0; i < netPolicyMap.size(); ++i) {
                polices[i] = netPolicyMap.keyAt(i);
                policyStrings[i] = netPolicyMap.valueAt(i);
            }
            new SearchableFlagsDialogBuilder<>(this, polices, policyStrings, selectedPolicies)
                    .setTitle(R.string.net_policy)
                    .showSelectAll(false)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.apply, (dialog, which, selections) -> {
                        int flags = 0;
                        for (int flag : selections) {
                            flags |= flag;
                        }
                        BatchNetPolicyOptions options = new BatchNetPolicyOptions(flags);
                        handleBatchOp(BatchOpsManager.OP_NET_POLICY, options);
                    })
                    .show();
        } else if (id == R.id.action_optimize) {
            DexOptDialog dialog = DexOptDialog.getInstance(viewModel.getSelectedPackages().keySet().toArray(new String[0]));
            dialog.show(getSupportFragmentManager(), DexOptDialog.TAG);
        } else if (id == R.id.action_export_blocking_rules) {
            final String fileName = "app_manager_rules_export-"
                    + DateUtils.formatDateTimeForFilename(this, System.currentTimeMillis()) + ".am.tsv";
            mBatchExportRules.launch(fileName);
        } else if (id == R.id.action_export_app_list) {
            List<Integer> exportTypes = Arrays.asList(ListExporter.EXPORT_TYPE_CSV,
                    ListExporter.EXPORT_TYPE_JSON,
                    ListExporter.EXPORT_TYPE_XML,
                    ListExporter.EXPORT_TYPE_MARKDOWN);
            new SearchableSingleChoiceDialogBuilder<>(this, exportTypes, R.array.export_app_list_options)
                    .setTitle(R.string.export_app_list_select_format)
                    .setOnSingleChoiceClickListener((dialog, which, item1, isChecked) -> {
                        if (!isChecked) {
                            return;
                        }
                        String filename = "app_manager_app_list-"
                                + DateUtils.formatLongDateTimeForFilename(this, System.currentTimeMillis()) + ".am";
                        switch (item1) {
                            case ListExporter.EXPORT_TYPE_CSV:
                                mExportAppListCsv.launch(filename + ".csv");
                                break;
                            case ListExporter.EXPORT_TYPE_JSON:
                                mExportAppListJson.launch(filename + ".json");
                                break;
                            case ListExporter.EXPORT_TYPE_XML:
                                mExportAppListXml.launch(filename + ".xml");
                                break;
                            case ListExporter.EXPORT_TYPE_MARKDOWN:
                                mExportAppListMarkdown.launch(filename + ".md");
                                break;
                        }
                    })
                    .setNegativeButton(R.string.close, null)
                    .show();
        } else if (id == R.id.action_force_stop) {
            handleBatchOp(BatchOpsManager.OP_FORCE_STOP);
        } else if (id == R.id.action_uninstall) {
            handleBatchOpWithWarning(BatchOpsManager.OP_UNINSTALL);
        } else if (id == R.id.action_add_to_profile) {
            AddToProfileDialogFragment dialog = AddToProfileDialogFragment.getInstance(viewModel.getSelectedPackages()
                    .keySet().toArray(new String[0]));
            dialog.show(getSupportFragmentManager(), AddToProfileDialogFragment.TAG);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onRefresh() {
        showProgressIndicator(true);
        if (viewModel != null) viewModel.loadApplicationItems();
        mSwipeRefresh.setRefreshing(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Set filter
        if (viewModel != null && mSearchView != null && !TextUtils.isEmpty(viewModel.getSearchQuery())) {
            if (mSearchView.isIconified()) {
                mSearchView.setIconified(false);
            }
            mSearchView.setQuery(viewModel.getSearchQuery(), false);
        }
        // Show/hide app usage menu
        if (mAppUsageMenu != null) {
            mAppUsageMenu.setVisible(FeatureController.isUsageAccessEnabled());
        }
        // Check for backup volume
        if (!Prefs.BackupRestore.backupDirectoryExists()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.backup_volume)
                    .setMessage(R.string.backup_volume_unavailable_warning)
                    .setPositiveButton(R.string.close, null)
                    .setNeutralButton(R.string.change_backup_volume, (dialog, which) -> {
                        Intent intent = SettingsActivity.getSettingsIntent(this, "backup_restore_prefs", "backup_volume");
                        startActivity(intent);
                    })
                    .show();
        }
    }

    /**
     * Safe installer management entry point. This does not install Xposed hooks or
     * intercept PackageInstaller sessions; interception must remain opt-in.
     */
    private final androidx.activity.result.ActivityResultLauncher<String[]> appSphereApkPicker =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) dispatchAppSphereApk(uri);
                    });

    private void dispatchAppSphereApk(@NonNull android.net.Uri uri) {
        String preferred = getSharedPreferences("appsphere_installer_control", MODE_PRIVATE)
                .getString("preferred_installer", "");
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!preferred.isEmpty()) {
            install.setPackage(preferred);
        }
        try {
            startActivity(install);
        } catch (android.content.ActivityNotFoundException | SecurityException ex) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Installer unavailable")
                    .setMessage("The selected installer cannot open this APK. Use Android's installer instead?")
                    .setPositiveButton("Use system installer", (dialog, which) -> {
                        Intent fallback = new Intent(Intent.ACTION_VIEW);
                        fallback.setDataAndType(uri, "application/vnd.android.package-archive");
                        fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            startActivity(Intent.createChooser(fallback, "Install APK with"));
                        } catch (Exception ignored) {
                            new MaterialAlertDialogBuilder(this)
                                    .setMessage("No compatible APK installer is available.")
                                    .setPositiveButton(android.R.string.ok, null).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private void showAppSphereInstallerControl() {
        final android.content.SharedPreferences prefs = getSharedPreferences(
                "appsphere_installer_control", MODE_PRIVATE);
        final String selected = prefs.getString("preferred_installer", "");
        final android.widget.LinearLayout panel = new android.widget.LinearLayout(this);
        panel.setOrientation(android.widget.LinearLayout.VERTICAL);
        final int pad = (int) (20 * getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad / 2, pad, pad / 2);

        final android.widget.TextView status = new android.widget.TextView(this);
        status.setText("Preferred installer: " + (selected.isEmpty() ? "System default" : selected)
                + "\\nAPK files can be opened here. System-wide redirection and LSPosed hooks are not active.");
        status.setTextSize(14);
        status.setPadding(0, 0, 0, pad / 2);
        panel.addView(status);

        final String[] actions = {
                "Install APK from storage",
                "Choose preferred installer",
                "Android default app settings",
                "Open InxLocker",
                "Reset installer preference"
        };
        final android.widget.LinearLayout actionList = new android.widget.LinearLayout(this);
        actionList.setOrientation(android.widget.LinearLayout.VERTICAL);
        panel.addView(actionList);
        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Installer Control")
                .setView(panel)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        for (int i = 0; i < actions.length; i++) {
            final int action = i;
            final com.google.android.material.button.MaterialButton button =
                    new com.google.android.material.button.MaterialButton(this);
            button.setText(actions[i]);
            button.setAllCaps(false);
            button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            actionList.addView(button, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            button.setOnClickListener(v -> {
                dialog.dismiss();
                if (action == 0) {
                    appSphereApkPicker.launch(new String[]{
                            "application/vnd.android.package-archive", "application/octet-stream"
                    });
                } else if (action == 1) {
                    chooseAppSphereInstaller();
                } else if (action == 2) {
                    try {
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
                    } catch (android.content.ActivityNotFoundException ex) {
                        new MaterialAlertDialogBuilder(this)
                                .setMessage("Default app settings are not available on this device.")
                                .setPositiveButton(android.R.string.ok, null).show();
                    }
                } else if (action == 3) {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(
                            "com.chimioo.inxlocker");
                    if (launch != null) startActivity(launch);
                    else new MaterialAlertDialogBuilder(this)
                            .setMessage("InxLocker is not available under the expected package name.")
                            .setPositiveButton(android.R.string.ok, null).show();
                } else {
                    prefs.edit().remove("preferred_installer").apply();
                    showAppSphereInstallerControl();
                }
            });
        }
        dialog.show();
    }

    private void chooseAppSphereInstaller() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(android.net.Uri.parse("content://appsphere.invalid/sample.apk"),
                "application/vnd.android.package-archive");
        java.util.List<android.content.pm.ResolveInfo> handlers =
                getPackageManager().queryIntentActivities(intent, 0);
        java.util.LinkedHashMap<String, String> candidates = new java.util.LinkedHashMap<>();
        for (android.content.pm.ResolveInfo handler : handlers) {
            if (handler.activityInfo == null) continue;
            String pkg = handler.activityInfo.packageName;
            CharSequence label = handler.loadLabel(getPackageManager());
            candidates.put(pkg, label + " (" + pkg + ")");
        }
        if (candidates.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage("No APK installer activities were visible. Android may restrict package visibility.")
                    .setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        final String[] packages = candidates.keySet().toArray(new String[0]);
        final String[] labels = candidates.values().toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Preferred installer")
                .setItems(labels, (dialog, which) -> {
                    getSharedPreferences("appsphere_installer_control", MODE_PRIVATE)
                            .edit().putString("preferred_installer", packages[which]).apply();
                    showAppSphereInstallerControl();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

        private void updateAppSphereSystemActivity() {
        android.widget.TextView stats = findViewById(R.id.appsphere_activity_stats);
        if (stats == null) return;
        android.app.ActivityManager manager = (android.app.ActivityManager)
                getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memory =
                new android.app.ActivityManager.MemoryInfo();
        manager.getMemoryInfo(memory);
        android.os.StatFs storage = new android.os.StatFs(
                android.os.Environment.getDataDirectory().getAbsolutePath());
        long gib = 1024L * 1024L * 1024L;
        stats.setText("Available RAM: " + (memory.availMem / gib) + " GiB / "
                + (memory.totalMem / gib) + " GiB"
                + "\nFree internal storage: " + (storage.getAvailableBytes() / gib)
                + " GiB / " + (storage.getTotalBytes() / gib) + " GiB"
                + "\nUpdated: " + java.text.DateFormat.getTimeInstance().format(
                        new java.util.Date()));
    }

    private void updateAppSphereBackendStatus() {
        android.widget.TextView status = findViewById(R.id.appsphere_backend_status);
        if (status == null) return;
        int uid = Ops.getWorkingUid();
        String route = Ops.isDirectRoot() ? "Root" : Ops.isAdb() ? "ADB" :
                Ops.isSystem() ? "System" : "Unprivileged";
        status.setText("Active route: " + route + "\nWorking UID: " + uid
                + "\nRoot available: " + (Ops.hasRoot() ? "Yes" : "No")
                + "\nConfigured mode: " + Ops.getMode());
    }

    private void applyAppSphereInterfaceStyle() {
        String style = getSharedPreferences("appsphere_appearance", MODE_PRIVATE)
                .getString("interface_style", "glass");
        View root = findViewById(R.id.coordinator_layout);
        View bar = findViewById(R.id.appsphere_floating_bar);
        View toolbar = findViewById(R.id.toolbar);
        if (root == null || bar == null || toolbar == null) return;
        if ("material".equals(style)) {
            android.util.TypedValue value = new android.util.TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, value, true);
            root.setBackgroundColor(value.data);
            toolbar.setBackgroundColor(value.data);
            bar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(value.data));
        } else if ("miuix".equals(style)) {
            root.setBackgroundColor(android.graphics.Color.rgb(17, 18, 24));
            toolbar.setBackgroundColor(android.graphics.Color.rgb(34, 36, 47));
            bar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.rgb(49, 52, 68)));
        } else {
            root.setBackgroundColor(android.graphics.Color.rgb(13, 12, 18));
            toolbar.setBackgroundColor(android.graphics.Color.argb(217, 28, 26, 37));
            bar.setBackgroundTintList(null);
        }
    }

    private boolean mImsVisibleBeforeLeave = false;

    @Override
    protected void onResume() {
        super.onResume();
        applyAppSphereInterfaceStyle();
        updateAppSphereBackendStatus();
        if (viewModel != null) viewModel.onResume();
        if (mAdapter != null && mBatchOpsHandler != null && mAdapter.isInSelectionMode()) {
            mBatchOpsHandler.updateConstraints();
            mMultiSelectionView.updateCounter(false);
        }
        if (mSearchView != null) {
            mSearchView.post(() -> {
                if (mImsVisibleBeforeLeave) {
                    mSearchView.requestFocus();
                    UiUtils.showKeyboard(mSearchView);
                }
            });
        }
        ContextCompat.registerReceiver(this, mBatchOpsBroadCastReceiver,
                new IntentFilter(BatchOpsService.ACTION_BATCH_OPS_COMPLETED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(getWindow().getDecorView());
        mImsVisibleBeforeLeave = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        View focusedView = getCurrentFocus();
        if (focusedView instanceof SearchView.SearchAutoComplete) {
            focusedView.clearFocus();
        }
        super.onPause();
        unregisterReceiver(mBatchOpsBroadCastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSearchDebouncer != null) {
            mSearchDebouncer.unbind();
        }
    }

    private void displayChangelogIfRequired() {
        if (!AppPref.getBoolean(AppPref.PrefKey.PREF_DISPLAY_CHANGELOG_BOOL)) {
            return;
        }
        if (FundingCampaignChecker.campaignRunning()) {
            new ScrollableDialogBuilder(this)
                    .setMessage(R.string.funding_campaign_dialog_message)
                    .enableAnchors()
                    .show();
        }
        Snackbar.make(findViewById(android.R.id.content), R.string.view_changelog, 3 * 60 * 1000)
                .setAction(R.string.ok, v -> {
                    long lastVersion = AppPref.getLong(AppPref.PrefKey.PREF_DISPLAY_CHANGELOG_LAST_VERSION_LONG);
                    AppPref.set(AppPref.PrefKey.PREF_DISPLAY_CHANGELOG_BOOL, false);
                    AppPref.set(AppPref.PrefKey.PREF_DISPLAY_CHANGELOG_LAST_VERSION_LONG, (long) BuildConfig.VERSION_CODE);
                    viewModel.executor.submit(() -> {
                        Changelog changelog;
                        try {
                            changelog = new ChangelogParser(getApplication(), R.raw.changelog, lastVersion).parse();
                        } catch (IOException | XmlPullParserException e) {
                            return;
                        }
                        runOnUiThread(() -> {
                            View view = View.inflate(this, R.layout.dialog_whats_new, null);
                            RecyclerView recyclerView = view.findViewById(android.R.id.list);
                            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
                            ChangelogRecyclerAdapter adapter = new ChangelogRecyclerAdapter();
                            recyclerView.setAdapter(adapter);
                            adapter.submitList(changelog.getChangelogItems());
                            new AlertDialogBuilder(this, true)
                                    .setTitle(R.string.changelog)
                                    .setView(recyclerView)
                                    .show();
                        });
                    });
                }).show();
    }

    private void showFreezeUnfreezeDialog(int freezeType) {
        View view = View.inflate(this, R.layout.item_checkbox, null);
        MaterialCheckBox checkBox = view.findViewById(R.id.checkbox);
        checkBox.setText(R.string.freeze_prefer_per_app_option);
        FreezeUnfreeze.getFreezeDialog(this, freezeType)
                .setIcon(R.drawable.ic_snowflake)
                .setTitle(R.string.freeze_unfreeze)
                .setView(view)
                .setPositiveButton(R.string.freeze, (dialog, which, selectedItem) -> {
                    if (selectedItem == null) {
                        return;
                    }
                    BatchFreezeOptions options = new BatchFreezeOptions(selectedItem, checkBox.isChecked());
                    handleBatchOp(BatchOpsManager.OP_ADVANCED_FREEZE, options);
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.unfreeze, (dialog, which, selectedItem) ->
                        handleBatchOp(BatchOpsManager.OP_UNFREEZE))
                .show();
    }

    private void handleBatchOp(@BatchOpsManager.OpType int op) {
        handleBatchOp(op, null);
    }

    private void handleBatchOp(@BatchOpsManager.OpType int op, @Nullable IBatchOpOptions options) {
        if (viewModel == null) return;
        showProgressIndicator(true);
        BatchOpsManager.Result input = new BatchOpsManager.Result(viewModel.getSelectedPackagesWithUsers());
        BatchQueueItem item = BatchQueueItem.getBatchOpQueue(op, input.getFailedPackages(), input.getAssociatedUsers(), options);
        Intent intent = BatchOpsService.getServiceIntent(this, item);
        ContextCompat.startForegroundService(this, intent);
    }

    private void handleBatchOpWithWarning(@BatchOpsManager.OpType int op) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.this_action_cannot_be_undone)
                .setPositiveButton(R.string.yes, (dialog, which) -> handleBatchOp(op))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    void showProgressIndicator(boolean show) {
        if (show) mProgressIndicator.show();
        else mProgressIndicator.hide();
    }
}
