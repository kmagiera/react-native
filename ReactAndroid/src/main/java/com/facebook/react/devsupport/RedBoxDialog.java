/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.devsupport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.R;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.devsupport.RedBoxHandler.ReportCompletedListener;
import com.facebook.react.devsupport.interfaces.StackFrame;

import org.json.JSONObject;

import javax.annotation.Nullable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Dialog for displaying JS errors in an eye-catching form (red box).
 */
/* package */ class RedBoxDialog extends Dialog {

  private final DoubleTapReloadRecognizer mDoubleTapReloadRecognizer;
  private final @Nullable RedBoxHandler mRedBoxHandler;

  private ListView mStackView;
  private @Nullable String mLastErrorTitle;
  private @Nullable StackFrame[] mLastErrorStack;
  private @Nullable Button mReportButton;
  private @Nullable TextView mReportTextView;
  private @Nullable ProgressBar mLoadingIndicator;
  private @Nullable View mLineSeparator;
  private boolean isReporting = false;

  private ReportCompletedListener mReportCompletedListener = new ReportCompletedListener() {
    @Override
    public void onReportSuccess(final SpannedString spannedString) {
      isReporting = false;
      Assertions.assertNotNull(mReportButton).setEnabled(true);
      Assertions.assertNotNull(mLoadingIndicator).setVisibility(View.GONE);
      Assertions.assertNotNull(mReportTextView).setText(spannedString);
    }
    @Override
    public void onReportError(final SpannedString spannedString) {
      isReporting = false;
      Assertions.assertNotNull(mReportButton).setEnabled(true);
      Assertions.assertNotNull(mLoadingIndicator).setVisibility(View.GONE);
      Assertions.assertNotNull(mReportTextView).setText(spannedString);
    }
  };

  private static class StackAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TITLE = 0;
    private static final int VIEW_TYPE_STACKFRAME = 1;

    private final String mTitle;
    private final StackFrame[] mStack;

    private static class FrameViewHolder {
      private final TextView mMethodView;
      private final TextView mFileView;

      private FrameViewHolder(View v) {
        mMethodView = (TextView) v.findViewById(R.id.rn_frame_method);
        mFileView = (TextView) v.findViewById(R.id.rn_frame_file);
      }
    }

    public StackAdapter(String title, StackFrame[] stack) {
      mTitle = title;
      mStack = stack;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public boolean isEnabled(int position) {
      return position > 0;
    }

    @Override
    public int getCount() {
      return mStack.length + 1;
    }

    @Override
    public Object getItem(int position) {
      return position == 0 ? mTitle : mStack[position - 1];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getViewTypeCount() {
      return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
      return position == 0 ? VIEW_TYPE_TITLE : VIEW_TYPE_STACKFRAME;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (position == 0) {
        TextView title = convertView != null
            ? (TextView) convertView
            : (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.redbox_item_title, parent, false);
        title.setText(mTitle);
        return title;
      } else {
        if (convertView == null) {
          convertView = LayoutInflater.from(parent.getContext())
              .inflate(R.layout.redbox_item_frame, parent, false);
          convertView.setTag(new FrameViewHolder(convertView));
        }
        StackFrame frame = mStack[position - 1];
        FrameViewHolder holder = (FrameViewHolder) convertView.getTag();
        holder.mMethodView.setText(frame.getMethod());
        holder.mFileView.setText(StackTraceHelper.formatFrameSource(frame));
        return convertView;
      }
    }
  }

  private static class OpenStackFrameTask extends AsyncTask<StackFrame, Void, Void> {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String mPackagerSourceUrl;

    private OpenStackFrameTask(String packagerSourceUrl) {
      mPackagerSourceUrl = packagerSourceUrl;
    }

    @Override
    protected Void doInBackground(StackFrame... stackFrames) {
      try {
        String openStackFrameUrl =
            Uri.parse(mPackagerSourceUrl).buildUpon()
                .path("/open-stack-frame")
                .query(null)
                .build()
                .toString();
        OkHttpClient client = new OkHttpClient();
        for (StackFrame frame: stackFrames) {
          String payload = stackFrameToJson(frame).toString();
          RequestBody body = RequestBody.create(JSON, payload);
          Request request = new Request.Builder().url(openStackFrameUrl).post(body).build();
          client.newCall(request).execute();
        }
      } catch (Exception e) {
        FLog.e(ReactConstants.TAG, "Could not open stack frame", e);
      }
      return null;
    }

    private static JSONObject stackFrameToJson(StackFrame frame) {
      return new JSONObject(
          MapBuilder.of(
              "file", frame.getFile(),
              "methodName", frame.getMethod(),
              "lineNumber", frame.getLine(),
              "column", frame.getColumn()
          ));
    }
  }

  private static class CopyToHostClipBoardTask extends AsyncTask<String, Void, Void> {
    private final String mPackagerSourceUrl;

    private CopyToHostClipBoardTask(String packagerSourceUrl) {
      mPackagerSourceUrl = packagerSourceUrl;
    }

    @Override
    protected Void doInBackground(String... clipBoardString) {
      try {
        String sendClipBoardUrl =
            Uri.parse(mPackagerSourceUrl).buildUpon()
                .path("/copy-to-clipboard")
                .query(null)
                .build()
                .toString();
        for (String string: clipBoardString) {
          OkHttpClient client = new OkHttpClient();
          RequestBody body = RequestBody.create(null, string);
          Request request = new Request.Builder().url(sendClipBoardUrl).post(body).build();
          client.newCall(request).execute();
        }
      } catch (Exception e) {
        FLog.e(ReactConstants.TAG, "Could not copy to the host clipboard", e);
      }
      return null;
    }
  }

  protected RedBoxDialog(
    Context context,
    final String packagerSourceUrl,
    @Nullable RedBoxHandler redBoxHandler) {
    super(context, R.style.Theme_Catalyst_RedBox);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.redbox_view);

    mDoubleTapReloadRecognizer = new DoubleTapReloadRecognizer();
    mRedBoxHandler = redBoxHandler;

    mStackView = (ListView) findViewById(R.id.rn_redbox_stack);
    mStackView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        new OpenStackFrameTask(packagerSourceUrl).executeOnExecutor(
        AsyncTask.THREAD_POOL_EXECUTOR,
        (StackFrame) mStackView.getAdapter().getItem(position));
      }
    });

    Button reloadJsButton = (Button) findViewById(R.id.rn_redbox_reload_button);
    reloadJsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setAction(getContext().getPackageName() + ".DEV_SUPPORT_ACTION");
        intent.putExtra("reload", true);
        getContext().sendBroadcast(intent);
      }
    });
    Button dismissButton = (Button) findViewById(R.id.rn_redbox_dismiss_button);
    dismissButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
      }
    });
    Button copyToClipboardButton = (Button) findViewById(R.id.rn_redbox_copy_button);
    copyToClipboardButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String title = mLastErrorTitle;
        StackFrame[] stack = mLastErrorStack;
        Assertions.assertNotNull(title);
        Assertions.assertNotNull(stack);
        new CopyToHostClipBoardTask(packagerSourceUrl).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR,
            StackTraceHelper.formatStackTrace(title, stack));
      }
    });

    if (mRedBoxHandler != null && mRedBoxHandler.isReportEnabled()) {
      mLoadingIndicator = (ProgressBar) findViewById(R.id.rn_redbox_loading_indicator);
      mLineSeparator = findViewById(R.id.rn_redbox_line_separator);
      mReportTextView = (TextView) findViewById(R.id.rn_redbox_report_label);
      mReportTextView.setMovementMethod(LinkMovementMethod.getInstance());
      mReportTextView.setHighlightColor(Color.TRANSPARENT);
      mReportButton = (Button) findViewById(R.id.rn_redbox_report_button);
      mReportButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (mRedBoxHandler == null || !mRedBoxHandler.isReportEnabled() || isReporting) {
            return;
          }
          isReporting = true;
          Assertions.assertNotNull(mReportTextView).setText("Reporting...");
          Assertions.assertNotNull(mReportTextView).setVisibility(View.VISIBLE);
          Assertions.assertNotNull(mLoadingIndicator).setVisibility(View.VISIBLE);
          Assertions.assertNotNull(mLineSeparator).setVisibility(View.VISIBLE);
          Assertions.assertNotNull(mReportButton).setEnabled(false);

          String title = Assertions.assertNotNull(mLastErrorTitle);
          StackFrame[] stack = Assertions.assertNotNull(mLastErrorStack);

          mRedBoxHandler.reportRedbox(
                  title,
                  stack,
                  packagerSourceUrl,
                  Assertions.assertNotNull(mReportCompletedListener));
        }
      });
    }
  }

  public void setExceptionDetails(String title, StackFrame[] stack) {
    mLastErrorTitle = title;
    mLastErrorStack = stack;
    mStackView.setAdapter(new StackAdapter(title, stack));
  }

  /**
   * Show the report button, hide the report textview and the loading indicator.
   */
  public void resetReporting(boolean enabled) {
    if (mRedBoxHandler == null || !mRedBoxHandler.isReportEnabled()) {
      return;
    }
    isReporting = false;
    Assertions.assertNotNull(mReportTextView).setVisibility(View.GONE);
    Assertions.assertNotNull(mLoadingIndicator).setVisibility(View.GONE);
    Assertions.assertNotNull(mLineSeparator).setVisibility(View.GONE);
    Assertions.assertNotNull(mReportButton).setVisibility(
      enabled ? View.VISIBLE : View.GONE);
    Assertions.assertNotNull(mReportButton).setEnabled(true);
  }

//  @Override
//  public boolean onKeyUp(int keyCode, KeyEvent event) {
//    if (keyCode == KeyEvent.KEYCODE_MENU) {
//      mDevSupportManager.showDevOptionsDialog();
//      return true;
//    }
//    if (mDoubleTapReloadRecognizer.didDoubleTapR(keyCode, getCurrentFocus())) {
//      mDevSupportManager.handleReloadJS();
//    }
//    return super.onKeyUp(keyCode, event);
//  }
}
