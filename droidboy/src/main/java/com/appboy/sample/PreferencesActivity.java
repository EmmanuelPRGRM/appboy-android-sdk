package com.appboy.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.appboy.Appboy;
import com.appboy.Constants;
import com.appboy.models.outgoing.AttributionData;
import com.appboy.sample.util.LifecycleUtils;
import com.appboy.sample.util.RuntimePermissionUtils;
import com.appboy.support.StringUtils;
import com.appboy.ui.feed.AppboyFeedManager;
import com.appboy.ui.inappmessage.AppboyInAppMessageManager;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

public class PreferencesActivity extends PreferenceActivity {
  private static final String TAG = String.format("%s.%s", Constants.APPBOY_LOG_TAG_PREFIX, PreferencesActivity.class.getName());
  private int mAttributionUniqueInt = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    setContentView(R.layout.preference_wrapper_view);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle(getString(R.string.settings));

    toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onBackPressed();
      }
    });

    Preference dataFlushPreference = findPreference("data_flush");
    Preference requestInAppMessagePreference = findPreference("request_inappmessage");
    Preference setManualLocationPreference = findPreference("set_manual_location");
    Preference locationRuntimePermissionDialogPreference = findPreference("location_runtime_permission_dialog");
    Preference openSessionPreference = findPreference("open_session");
    Preference closeSessionPreference = findPreference("close_session");
    Preference anonymousUserRevertPreference = findPreference("anonymous_revert");
    Preference sdkPreference = findPreference("sdk_version");
    Preference apiKeyPreference = findPreference("api_key");
    Preference pushTokenPreference = findPreference("push_token");
    Preference externalStorageRuntimePermissionDialogPreference = findPreference("external_storage_runtime_permission_dialog");
    Preference toggleDisableAppboyNetworkRequestsPreference = findPreference("toggle_disable_appboy_network_requests_for_filtered_emulators");
    Preference logAttributionPreference = findPreference("log_attribution");
    Preference enableAutomaticNetworkRequestsPreference = findPreference("enable_outbound_network_requests");
    Preference disableAutomaticNetworkRequestsPreference = findPreference("disable_outbound_network_requests");
    CheckBoxPreference sortNewsFeed = (CheckBoxPreference) findPreference("sort_feed");
    SharedPreferences sharedPrefSort = getSharedPreferences(getString(R.string.feed), Context.MODE_PRIVATE);
    sortNewsFeed.setChecked(sharedPrefSort.getBoolean(getString(R.string.sort_feed), false));
    CheckBoxPreference setCustomNewsFeedClickActionListener = (CheckBoxPreference) findPreference("set_custom_news_feed_card_click_action_listener");

    sdkPreference.setSummary(Constants.APPBOY_SDK_VERSION);
    apiKeyPreference.setSummary(DroidboyApplication.getApiKeyInUse(getApplicationContext()));
    String pushToken = Appboy.getInstance(PreferencesActivity.this).getAppboyPushMessageRegistrationId();
    if (StringUtils.isNullOrBlank(pushToken)) {
      pushToken = "None";
    }
    pushTokenPreference.setSummary(pushToken);

    setManualLocationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Appboy.getInstance(PreferencesActivity.this).getCurrentUser().setLastKnownLocation(1.0, 2.0, 3.0, 4.0);
        showToast("Manually set location to latitude 1.0d, longitude 2.0d, altitude 3.0m, accuracy 4.0m.");
        return true;
      }
    });
    locationRuntimePermissionDialogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, RuntimePermissionUtils.DROIDBOY_PERMISSION_LOCATION);
        } else {
          Toast.makeText(PreferencesActivity.this, "Below Android M there is no need to check for runtime permissions.", Toast.LENGTH_SHORT).show();
        }
        return true;
      }
    });

    disableAutomaticNetworkRequestsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Appboy.setOutboundNetworkRequestsOffline(true);
        showToast(getString(R.string.disabled_outbound_network_requests_toast));
        return true;
      }
    });
    enableAutomaticNetworkRequestsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Appboy.setOutboundNetworkRequestsOffline(false);
        showToast(getString(R.string.enabled_outbound_network_requests_toast));
        return true;
      }
    });

    dataFlushPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Appboy.getInstance(PreferencesActivity.this).requestImmediateDataFlush();
        showToast(getString(R.string.data_flush_toast));
        return true;
      }
    });
    requestInAppMessagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Appboy.getInstance(PreferencesActivity.this).requestInAppMessageRefresh();
        showToast(getString(R.string.requested_inappmessage_toast));
        return true;
      }
    });
    openSessionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (Appboy.getInstance(PreferencesActivity.this).openSession(PreferencesActivity.this)) {
          showToast(getString(R.string.open_session_toast));
        } else {
          showToast(getString(R.string.resume_session_toast));
        }
        return true;
      }
    });
    closeSessionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (Appboy.getInstance(PreferencesActivity.this).closeSession(PreferencesActivity.this)) {
          showToast(getString(R.string.close_session_toast));
        } else {
          showToast(getString(R.string.no_session_toast));
        }
        return true;
      }
    });
    anonymousUserRevertPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      @SuppressLint("CommitPrefEdits")
      public boolean onPreferenceClick(Preference preference) {
        SharedPreferences userSharedPreferences = getSharedPreferences("com.appboy.offline.storagemap", Context.MODE_PRIVATE);
        userSharedPreferences
            .edit()
            .clear()
            .commit();
        SharedPreferences droidboySharedPrefs = getSharedPreferences("droidboy", Context.MODE_PRIVATE);
        droidboySharedPrefs
            .edit()
            .remove(MainFragment.USER_ID_KEY)
            .commit();
        LifecycleUtils.restartApp(getApplicationContext());
        return true;
      }
    });
    externalStorageRuntimePermissionDialogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RuntimePermissionUtils.DROIDBOY_PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
          Toast.makeText(PreferencesActivity.this, "Below Android M there is no need to check for runtime permissions.", Toast.LENGTH_SHORT).show();
        }
        return true;
      }
    });
    toggleDisableAppboyNetworkRequestsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        boolean newDisableAppboyNetworkRequestsPreference = !Boolean
            .parseBoolean(getApplicationContext().getSharedPreferences(
                getString(R.string.shared_prefs_location), Context.MODE_PRIVATE)
                .getString(getString(R.string.mock_appboy_network_requests), null));
        SharedPreferences.Editor sharedPreferencesEditor = getApplicationContext().getSharedPreferences(getString(R.string.shared_prefs_location), Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putBoolean(getString(R.string.mock_appboy_network_requests), newDisableAppboyNetworkRequestsPreference);
        sharedPreferencesEditor.apply();
        if (newDisableAppboyNetworkRequestsPreference) {
          Toast.makeText(PreferencesActivity.this, "Disabling Appboy network requests for selected emulators in the next app run", Toast.LENGTH_LONG).show();
        } else {
          Toast.makeText(PreferencesActivity.this, "Enabling Appboy network requests for the next app run for all devices", Toast.LENGTH_LONG).show();
        }
        return true;
      }
    });

    logAttributionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Appboy.getInstance(getApplicationContext()).getCurrentUser().setAttributionData(new AttributionData("network_val_" + mAttributionUniqueInt,
            "campaign_val_" + mAttributionUniqueInt,
            "adgroup_val_" + mAttributionUniqueInt,
            "creative_val_" + mAttributionUniqueInt));
        mAttributionUniqueInt++;
        showToast("Attribution data sent to server");
        return true;
      }
    });
    sortNewsFeed.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.feed), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.sort_feed), (boolean) newValue);
        editor.apply();
        return true;
      }
    });
    setCustomNewsFeedClickActionListener.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        AppboyFeedManager.getInstance().setFeedCardClickActionListener((boolean) newValue ? new CustomFeedClickActionListener() : null);
        return true;
      }
    });
  }

  // Displays a toast to the user
  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onStart() {
    super.onStart();
    // Opens a new Appboy session. You can now start logging custom events.
    Appboy.getInstance(this).openSession(this);

    Branch branch = Branch.getInstance();
    branch.initSession(new Branch.BranchReferralInitListener() {
      @Override
      public void onInitFinished(JSONObject referringParams, BranchError error) {
        if (error == null) {
          String param1 = referringParams.optString("$param_1", "");
          String param2 = referringParams.optString("$param_2", "");
          if (param1.equals("hello")) {
            showToast("This activity was opened by a Branch deep link with custom param 1.");
          } else if (param2.equals("goodbye")) {
            showToast("This activity was opened by a Branch deep link with custom param 2.");
          } else {
            showToast("This activity was opened by a Branch deep link with no custom params!");
          }
        } else {
          Log.i(TAG, error.getMessage());
        }
      }
    }, this.getIntent().getData(), this);
  }

  @Override
  public void onNewIntent(Intent intent) {
    this.setIntent(intent);
  }

  @Override
  public void onResume() {
    super.onResume();
    // Registers the AppboyInAppMessageManager for the current Activity. This Activity will now listen for
    // in-app messages from Appboy.
    AppboyInAppMessageManager.getInstance().registerInAppMessageManager(this);

    // Shows a toast if the activity detects that it was opened via a deep link.
    Bundle extras = getIntent().getExtras();
    if (extras != null && Constants.APPBOY.equals(extras.getString(AppboyBroadcastReceiver.SOURCE_KEY))) {
      showToast("This activity was opened by a deep link!");
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    // Unregisters the AppboyInAppMessageManager.
    AppboyInAppMessageManager.getInstance().unregisterInAppMessageManager(this);
  }

  @Override
  public void onStop() {
    super.onStop();
    // Closes the Appboy session.
    Appboy.getInstance(this).closeSession(this);
    Branch.getInstance(getApplicationContext()).closeSession();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    RuntimePermissionUtils.handleOnRequestPermissionsResult(PreferencesActivity.this, requestCode, grantResults);
  }
}
