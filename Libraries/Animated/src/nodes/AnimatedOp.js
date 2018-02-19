'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');

class AnimatedOp extends AnimatedWithInput {
  _inputNodes;
  _processor;

  constructor(inputNodes, processor) {
    super(inputNodes.filter(node => node instanceof AnimatedNode));
    this._inputNodes = inputNodes;
    this._processor = processor;
  }

  __onEvaluate() {
    return this._processor(this._inputNodes.map(node => (node instanceof AnimatedNode ? node.__getValue() : node)));
  }
}

module.exports = AnimatedOp;
