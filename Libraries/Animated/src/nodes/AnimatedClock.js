'use strict';

const AnimatedValue = require('./AnimatedValue');

class AnimatedClock extends AnimatedValue {
  _frameCallback;

  constructor() {
    super(0);
  }

  _runFrame = () => {
    this._updateValue(+new Date());
    if (this.__children.length > 0) {
      this._frameCallback = requestAnimationFrame(this._runFrame);
    }
  }

  __attach() {
    super.__attach();
    if (!this._frameCallback) {
      this._frameCallback = requestAnimationFrame(this._runFrame);
    }
  }

  __detach() {
    if (this._frameCallback) {
      cancelAnimationFrame(this._frameCallback);
      this._frameCallback = null;
    }
    super.__detach();
  }
}

const clock = new AnimatedClock();

module.exports = {
  clock
}
