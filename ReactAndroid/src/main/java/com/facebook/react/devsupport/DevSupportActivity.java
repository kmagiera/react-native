package com.facebook.react.devsupport;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.SpannedString;
import android.util.Log;

import com.facebook.react.devsupport.interfaces.StackFrame;

import javax.annotation.Nullable;

public class DevSupportActivity extends FragmentActivity {

  /*package*/ static String ACTION_EXTRA_CLOSE = "close";
  /*package*/ static String ACTION_EXTRA_DIALOG_OPTIONS = "options";
  /*package*/ static String ACTION_EXTRA_REDBOX_STACK = "stack";
  /*package*/ static String ACTION_EXTRA_REDBOX_TITLE = "errorTitle";
  /*package*/ static String ACTION_EXTRA_REDBOX_REPORTING = "reporting";
  /*package*/ static String ACTION_EXTRA_REDBOX_PACKAGER_URL = "packagerUrl";
  /*package*/ static String ACTION_EXTRA_REDBOX_REPORTING_SUCCEEDED = "reportSuccess";
  /*package*/ static String ACTION_EXTRA_REDBOX_REPORTING_ERROR = "reportFailure";
  /*package*/ static String ACTION_EXTRA_SETTINGS = "settings";
  /*package*/ static String ACTION_EXTRA_SHOW_LOADING = "loadingShow";
  /*package*/ static String ACTION_EXTRA_UPDATE_LOADING_PROGRESS = "loadingProgress";
  /*package*/ static String ACTION_EXTRA_UPDATE_LOADING_PROGRESS_DONE = "loadingProgressDone";
  /*package*/ static String ACTION_EXTRA_UPDATE_LOADING_PROGRESS_TOTAL = "loadingProgressTotal";
  /*package*/ static String ACTION_EXTRA_HIDE_LOADING = "loadingHide";

  private static String CALLBACK_ACTION_SUFFIX = ".DEV_SUPPORT_ACTION";

  public static String CALLBACK_DIALOG_OPTION_SELECTED = "selectedDevDialogOption";
  public static String CALLBACK_RELOAD_REQUESTED = "reload";
  public static String CALLBACK_REPORT_REQUEST = "report";

  /* package */ static Intent createCallbackIntent(Context applicationContext) {
    Intent intent = new Intent();
    intent.setAction(applicationContext.getPackageName() + CALLBACK_ACTION_SUFFIX);
    return intent;
  }

  /* package */ static Intent createLaunchIntent(Context applicationContext) {
    Intent intent = new Intent(applicationContext, DevSupportActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return intent;
  }

  /* package */ static void startRedboxDialog(
          Context applicationContext,
          String title,
          StackFrame[] stack,
          String packagerSourcUrl,
          boolean enableReporting) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_REDBOX_TITLE, title);
    intent.putExtra(ACTION_EXTRA_REDBOX_STACK, stack);
    intent.putExtra(ACTION_EXTRA_REDBOX_PACKAGER_URL, packagerSourcUrl);
    intent.putExtra(ACTION_EXTRA_REDBOX_REPORTING, enableReporting);
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevOptionsDialog(Context applicationContext, String[] options) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_DIALOG_OPTIONS, options);
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevSettings(Context applicationContext) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_SETTINGS, true);
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevLoadingShow(Context applicationContext) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_SHOW_LOADING, "show");
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevLoadingForRemoteJSEnabled(Context applicationContext) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_SHOW_LOADING, "remoteJS");
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevLoadingForUrl(Context applicationContext, String url) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_SHOW_LOADING, url);
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevLoadingUpdateProgress(
          Context applicationContext,
          String status,
          @Nullable Integer done,
          @Nullable Integer total) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS, status);
    if (done != null) {
      intent.putExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS_DONE, done);
    }
    if (total != null) {
      intent.putExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS_TOTAL, total);
    }
    applicationContext.startActivity(intent);
  }

  /* package */ static void startDevLoadingHide(Context applicationContext) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_HIDE_LOADING, true);
    applicationContext.startActivity(intent);
  }

  /* package */ static void closeAll(Context applicationContext) {
    Intent intent = createLaunchIntent(applicationContext);
    intent.putExtra(ACTION_EXTRA_CLOSE, true);
    applicationContext.startActivity(intent);
  }

  /* package */ void sendReloadRequest() {
    Intent intent = createCallbackIntent(this);
    intent.putExtra(CALLBACK_RELOAD_REQUESTED, true);
    sendBroadcast(intent);
  }

  /* package */ void sendReportRequest(String title, StackFrame[] stack) {
    Intent intent = createCallbackIntent(this);
    intent.putExtra(CALLBACK_REPORT_REQUEST, true);
    intent.putExtra(ACTION_EXTRA_REDBOX_TITLE, title);
    intent.putExtra(ACTION_EXTRA_REDBOX_STACK, stack);
    sendBroadcast(intent);
  }

  public static abstract class CallbackBroadcastReceiver extends BroadcastReceiver {

    public abstract void onDevOptionSelected(int which);
    public abstract void onReload();
    public abstract void onReportRequested(
            String title,
            StackFrame[] stack,
            RedBoxHandler.ReportCompletedListener listener);


    private Context mContext;

    public CallbackBroadcastReceiver(Context context) {
      mContext = context;
    }

    @Override
    final public void onReceive(Context context, Intent intent) {
      if (intent.hasExtra(CALLBACK_DIALOG_OPTION_SELECTED)) {
        int which = intent.getIntExtra(CALLBACK_DIALOG_OPTION_SELECTED, 0);
        onDevOptionSelected(which);
      } else if (intent.hasExtra(CALLBACK_RELOAD_REQUESTED)) {
        onReload();
      } else if (intent.hasExtra(CALLBACK_REPORT_REQUEST)) {
        String title = intent.getStringExtra(ACTION_EXTRA_REDBOX_TITLE);
        StackFrame[] stack = StackTraceHelper.stackFromParcelableArray(
                intent.getParcelableArrayExtra(ACTION_EXTRA_REDBOX_STACK));
        onReportRequested(title, stack, new RedBoxHandler.ReportCompletedListener() {
          @Override
          public void onReportSuccess(SpannedString spannedString) {
            Intent intent = createLaunchIntent(mContext);
            intent.putExtra(ACTION_EXTRA_REDBOX_REPORTING_SUCCEEDED, spannedString);
            mContext.startActivity(intent);
          }

          @Override
          public void onReportError(SpannedString spannedString) {
            Intent intent = createLaunchIntent(mContext);
            intent.putExtra(ACTION_EXTRA_REDBOX_REPORTING_ERROR, spannedString);
            mContext.startActivity(intent);
          }
        });
      }
    }

    public void register(Context applicationContext) {
      IntentFilter filter = new IntentFilter();
      filter.addAction(applicationContext.getPackageName() + CALLBACK_ACTION_SUFFIX);
      applicationContext.registerReceiver(this, filter);
    }

    public void unregister(Context applicationContext) {
      applicationContext.unregisterReceiver(this);
    }
  }

  private static String REDBOX_FRAGMENT_TAG = "redbox";
  private static String SETTINGS_FRAGMENT_TAG = "settings";
  private static String DIALOG_FRAGMENT_TAG = "dialog";
  private static String LOADING_FRAGMENT_TAG = "loading";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onNewIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.e("CAT", "NEW INTENT " + intent);
    if (intent.hasExtra(ACTION_EXTRA_CLOSE)) {
      finish();
    } else if (intent.hasExtra(ACTION_EXTRA_DIALOG_OPTIONS)) {
      showDevOptionsDialog(intent);
    } else if (intent.hasExtra(ACTION_EXTRA_REDBOX_STACK)) {
      showRedboxDialog(intent);
    } else if (intent.hasExtra(ACTION_EXTRA_SETTINGS)) {
      showSettings();
    } else if (intent.hasExtra(ACTION_EXTRA_REDBOX_REPORTING_SUCCEEDED)) {
      CharSequence message = intent.getCharSequenceExtra(ACTION_EXTRA_REDBOX_REPORTING_SUCCEEDED);
      RedBoxFragment redBoxFragment = (RedBoxFragment) getFragmentManager()
              .findFragmentByTag(REDBOX_FRAGMENT_TAG);
      if (redBoxFragment != null) {
        redBoxFragment.onReportSuccess(new SpannedString(message));
      }
    } else if (intent.hasExtra(ACTION_EXTRA_REDBOX_REPORTING_ERROR)) {
      CharSequence message = intent.getCharSequenceExtra(ACTION_EXTRA_REDBOX_REPORTING_ERROR);
      RedBoxFragment redBoxFragment = (RedBoxFragment) getFragmentManager()
              .findFragmentByTag(REDBOX_FRAGMENT_TAG);
      if (redBoxFragment != null) {
        redBoxFragment.onReportError(new SpannedString(message));
      }
    } else if (intent.hasExtra(ACTION_EXTRA_SHOW_LOADING)) {
      DevLoadingFragment frag = (DevLoadingFragment) getFragmentManager().findFragmentByTag(LOADING_FRAGMENT_TAG);
      if (frag == null) {
        frag = new DevLoadingFragment();
        frag.setArguments(intent.getExtras());
        getFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, frag, LOADING_FRAGMENT_TAG)
                .commit();
      } else {
        frag.getArguments().putAll(intent.getExtras());
        frag.update();
      }

    } else if (intent.hasExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS)) {
      String status = ACTION_EXTRA_UPDATE_LOADING_PROGRESS;
      Integer done = null;
      if (intent.hasExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS_DONE)) {
        done = intent.getIntExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS_DONE, 0);
      }
      Integer total = null;
      if (intent.hasExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS_TOTAL)) {
        total = intent.getIntExtra(ACTION_EXTRA_UPDATE_LOADING_PROGRESS_TOTAL, 0);
      }
      DevLoadingFragment frag = (DevLoadingFragment) getFragmentManager().findFragmentByTag(LOADING_FRAGMENT_TAG);
      if (frag != null) {
        frag.updateProgress(status, done, total);
      }
    } else if (intent.hasExtra(ACTION_EXTRA_HIDE_LOADING)) {
      Fragment frag = getFragmentManager().findFragmentByTag(LOADING_FRAGMENT_TAG);
      if (frag != null) {
        getFragmentManager().beginTransaction().remove(frag).commit();
        quitIfEmptyStack();
      }
    }
  }

  @Override
  public void onBackPressed() {
    Fragment topFragment = getFragmentManager().findFragmentById(android.R.id.content);

    if (topFragment instanceof DevLoadingFragment) {
      // dev loading at top – suppress back button
    } else if (topFragment == null) {
      // fragment stack is empty, call super to close down the activity
      super.onBackPressed();
    } else {
      // remove fragment
      getFragmentManager().beginTransaction().remove(topFragment).commit();
      quitIfEmptyStack();
    }
  }

  private void quitIfEmptyStack() {
    getFragmentManager().executePendingTransactions();
    Fragment topFragment = getFragmentManager().findFragmentById(android.R.id.content);
    if (topFragment == null) {
      finish();
    }
  }

  private void onDevOptionSelected(int which) {
    Intent intent = createCallbackIntent(this);
    intent.putExtra(CALLBACK_DIALOG_OPTION_SELECTED, which);
    sendBroadcast(intent);
  }

  private void showDevOptionsDialog(Intent intent) {
    String[] options = intent.getStringArrayExtra(ACTION_EXTRA_DIALOG_OPTIONS);
    new AlertDialog.Builder(this)
            .setItems(options,
                    new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        onDevOptionSelected(which);
                        DevSupportActivity.this.finish();
                      }
                    })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                DevSupportActivity.this.finish();
              }
            })
            .create().show();
  }

  private void showRedboxDialog(Intent intent) {
    RedBoxFragment redBoxFragment = (RedBoxFragment) getFragmentManager()
            .findFragmentByTag(REDBOX_FRAGMENT_TAG);
    if (redBoxFragment == null) {
      redBoxFragment = new RedBoxFragment();
      redBoxFragment.setArguments(intent.getExtras());
      getFragmentManager()
              .beginTransaction()
              .add(android.R.id.content, redBoxFragment, REDBOX_FRAGMENT_TAG)
              .commit();
    } else {
      redBoxFragment.getArguments().putAll(intent.getExtras());
      redBoxFragment.updateData();
    }
  }

  private void showSettings() {
    if (getFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT_TAG) == null) {
      getFragmentManager()
              .beginTransaction()
              .add(android.R.id.content, new DevSettingsFragment(), SETTINGS_FRAGMENT_TAG)
              .commit();
    }
  }
}
