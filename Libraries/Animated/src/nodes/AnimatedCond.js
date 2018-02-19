'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');

class AnimatedCond extends AnimatedWithInput {
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
    if (this._condition.__getValue ? this._condition.__getValue() : this._condition) {
      return this._ifBlock.__getValue ? this._ifBlock.__getValue() : this._ifBlock;
    } else {
      return this._elseBlock.__getValue ? this._elseBlock.__getValue() : this._elseBlock;
    }
  }
}

module.exports = AnimatedCond;
