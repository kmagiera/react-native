'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');

function val(v) {
  return v.__getValue ? v.__getValue() : v;
}

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
    if (val(this._condition)) {
      return val(this._ifBlock);
    } else {
      return val(this._elseBlock);
    }
  }
}

module.exports = AnimatedCond;
