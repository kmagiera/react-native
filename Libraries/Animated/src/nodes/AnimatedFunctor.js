'use strict';

const AnimatedWithChildren = require('./AnimatedWithChildren');

class AnimatedFunctor extends AnimatedWithChildren {
  _inputNodes;
  _processor;

  constructor(inputNodes, processor) {
    super(inputNodes);
    this._inputNodes = inputNodes;
    this._processor = processor;
  }

  __onEvaluate() {
    return this._processor(this._inputNodes.map(node => node.__getValue()));
  }
}

module.exports = AnimatedFunctor;
