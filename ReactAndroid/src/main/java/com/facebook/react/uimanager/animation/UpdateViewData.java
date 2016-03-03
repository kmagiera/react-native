package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableMap;

/*package*/ class UpdateViewData {
  int mViewTag;
  ReadableMap mProps;

  public UpdateViewData(int tag, ReadableMap props) {
    mViewTag = tag;
    mProps = props;
  }
}
