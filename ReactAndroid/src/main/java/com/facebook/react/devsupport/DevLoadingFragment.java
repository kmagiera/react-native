/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.devsupport;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.common.logging.FLog;
import com.facebook.react.R;
import com.facebook.react.common.ReactConstants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Controller to display loading messages on top of the screen. All methods are thread safe.
 */
public class DevLoadingFragment extends Fragment {
  private static final int COLOR_DARK_GREEN = Color.parseColor("#035900");

  private TextView mDevLoadingView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dev_loading_view, container, false);
    mDevLoadingView = (TextView) view.findViewById(R.id.dev_loading_text);

    update();

    return view;
  }

  /*package*/ void update() {
    String urlOrOption = getArguments().getString(DevSupportActivity.ACTION_EXTRA_SHOW_LOADING);
    if ("show".equals(urlOrOption)) {
      // do nothing
    } else if ("remoteJS".equals(urlOrOption)) {
      showForRemoteJSEnabled();
    } else {
      showForUrl(urlOrOption);
    }
  }

  public void showMessage(final String message, final int color, final int backgroundColor) {
    mDevLoadingView.setBackgroundColor(backgroundColor);
    mDevLoadingView.setText(message);
    mDevLoadingView.setTextColor(color);
  }

  public void showForUrl(String url) {
    URL parsedURL;
    try {
      parsedURL = new URL(url);
    } catch (MalformedURLException e) {
      FLog.e(ReactConstants.TAG, "Bundle url format is invalid. \n\n" + e.toString());
      return;
    }

    showMessage(
      getString(R.string.catalyst_loading_from_url, parsedURL.getHost() + ":" + parsedURL.getPort()),
      Color.WHITE,
      COLOR_DARK_GREEN);
  }

  public void showForRemoteJSEnabled() {
    showMessage(getString(R.string.catalyst_remotedbg_message), Color.WHITE, COLOR_DARK_GREEN);
  }

  public void updateProgress(final @Nullable String status, final @Nullable Integer done, final @Nullable Integer total) {
    StringBuilder message = new StringBuilder();
    message.append(status != null ? status : "Loading");
    if (done != null && total != null && total > 0) {
      message.append(String.format(Locale.getDefault(), " %.1f%% (%d/%d)", (float) done / total * 100, done, total));
    }
    message.append("\u2026"); // `...` character

    mDevLoadingView.setText(message);
  }
}
