package com.facebook.react.devsupport;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.facebook.react.R;
import com.facebook.react.devsupport.interfaces.StackFrame;

public class DevSupportActivity extends FragmentActivity {

  private static String ACTION_EXTRA_CLOSE = "close";
  private static String ACTION_EXTRA_DIALOG_OPTIONS = "options";
  private static String ACTION_EXTRA_REDBOX_STACK = "stack";
  private static String ACTION_EXTRA_REDBOX_TITLE = "errorTitle";
  private static String ACTION_EXTRA_REDBOX_REPORTING = "reporting";
  private static String ACTION_EXTRA_SETTINGS = "settings";

  private static String CALLBACK_ACTION_SUFFIX = ".DEV_SUPPORT_ACTION";

  private static String CALLBACK_DIALOG_OPTION_SELECTED = "selectedDevDialogOption";
  private static String CALLBACK_RELOAD_REQUESTED = "reload";

  public static Intent createCallbackIntent(Context applicationContext) {
    Intent intent = new Intent();
    intent.setAction(applicationContext.getPackageName() + CALLBACK_ACTION_SUFFIX);
  }

  public static Intent createLaunchIntent(Context applicationContext) {
    Intent intent = new Intent(applicationContext, DevSupportActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return intent;
  }

  public static abstract class CallbackBroadcastReceiver extends BroadcastReceiver {

    public abstract void onDevOptionSelected(int which);
    public abstract void onReload();

    @Override
    final public void onReceive(Context context, Intent intent) {
      if (intent.hasExtra(CALLBACK_DIALOG_OPTION_SELECTED)) {
        int which = intent.getIntExtra(CALLBACK_DIALOG_OPTION_SELECTED, 0);
        onDevOptionSelected(which);
      } else if (intent.hasExtra(CALLBACK_RELOAD_REQUESTED)) {
        onReload();
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

  RedBoxDialog mRedBoxDialog;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_Catalyst_DevSupport);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    if (intent.hasExtra(ACTION_EXTRA_CLOSE)) {
      finish();
    } else if (intent.hasExtra(ACTION_EXTRA_DIALOG_OPTIONS)) {
      showDevOptionsDialog(intent);
    } else if (intent.hasExtra(ACTION_EXTRA_REDBOX_STACK)) {
      showRedboxDialog(intent);
    } else if (intent.hasExtra(ACTION_EXTRA_SETTINGS)) {

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
    String title = intent.getStringExtra(ACTION_EXTRA_REDBOX_TITLE);
    Parcelable[] parcelableStack = intent.getParcelableArrayExtra(ACTION_EXTRA_REDBOX_STACK);
    StackFrame[] stack = new StackFrame[parcelableStack.length];
    for (int i = 0; i < parcelableStack.length; i++) {
      stack[i] = (StackFrame) parcelableStack[i];
    }
    boolean reporting = intent.getBooleanExtra(ACTION_EXTRA_REDBOX_REPORTING, false);

    if (mRedBoxDialog == null) {
      mRedBoxDialog = new RedBoxDialog(this, null, null);
    }
    mRedBoxDialog.setExceptionDetails(title, stack);
    mRedBoxDialog.resetReporting(reporting);
    mRedBoxDialog.show();
  }
}
