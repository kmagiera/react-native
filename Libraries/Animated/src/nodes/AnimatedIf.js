'use strict';

const AnimatedWithChildren = require('./AnimatedWithChildren');

class AnimatedIf extends AnimatedWithChildren {
  _condition;
  _ifBlock;
  _elseBlock;

  constructor(condition, ifBlock, elseBlock) {
    super([condition, ifBlock, elseBlock]);
    this._condition = condition;
    this._ifBlock = ifBlock;
    this._elseBlock = elseBlock;
  }

  __onEvaluate() {
    if (this._condition.__getValue()) {
      return this._ifBlock.__getValue();
    } else {
      return this._elseBlock.__getValue();
    }
  }
}

module.exports = AnimatedIf;
