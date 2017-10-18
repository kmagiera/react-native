package com.facebook.react.devsupport;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.facebook.react.devsupport.interfaces.StackFrame;

public class DevSupportActivity extends FragmentActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    if (intent.hasExtra("options")) {
      showDevOptionsDialog();
    } else if (intent.hasExtra("stack")) {
      showRedboxDialog();
    } else if (intent.hasExtra("settings")) {

    }
  }

  private void onDevOptionSelected(int which) {
    Intent intent = new Intent();
    intent.setAction(getPackageName() + ".DEV_SUPPORT_ACTION");
    intent.putExtra("selectedDevDialogOption", which);
    sendBroadcast(intent);
  }

  private void showDevOptionsDialog() {
    String[] options = getIntent().getStringArrayExtra("options");
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

  private void showRedboxDialog() {
    try {
      Log.e("CAT", "BLABLA");
      RedBoxDialog redBoxDialog = new RedBoxDialog(this, null, null);
      String title = getIntent().getStringExtra("errorTitle");
      StackFrame[] stack = (StackFrame[]) getIntent().getParcelableArrayExtra("stack");
      boolean reporting = getIntent().getBooleanExtra("reporting", false);
      redBoxDialog.setExceptionDetails(title, stack);
      redBoxDialog.resetReporting(reporting);
      redBoxDialog.show();
      Log.e("CAT", "DONE");
    } catch (Throwable t) {
      Log.e("CAT", "Trouble", t);
    }
  }
}
