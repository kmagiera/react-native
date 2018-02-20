'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');

const {clock} = require('./AnimatedClock');

function spring(state, config) {
  let now = val(clock);
  const lastTime = state.time || now;
  const frameTime = state.frameTime;
  const lastPosition = state.position;
  const lastVelocity = state.velocity;

  const MAX_STEPS = 64;
  if (now > lastTime + MAX_STEPS) {
    now = lastTime + MAX_STEPS;
  }

  const deltaTime = (now - lastTime) / 1000;

  const c = config.damping;
  const m = config.mass;
  const k = config.stiffness;
  const v0 = -lastVelocity;
  const x0 = config.toValue - lastPosition;

  const zeta = c / (2 * Math.sqrt(k * m)); // damping ratio
  const omega0 = Math.sqrt(k / m); // undamped angular frequency of the oscillator (rad/ms)
  const omega1 = omega0 * Math.sqrt(1.0 - zeta * zeta); // exponential decay

  let position = 0.0;
  let velocity = 0.0;
  const t = frameTime + deltaTime;
  if (zeta < 1) {
    // Under damped
    const envelope = Math.exp(-zeta * omega0 * t);
    position =
      config.toValue -
      envelope *
        ((v0 + zeta * omega0 * x0) / omega1 * Math.sin(omega1 * t) +
          x0 * Math.cos(omega1 * t));
    // This looks crazy -- it's actually just the derivative of the
    // oscillation function
    velocity =
      zeta *
        omega0 *
        envelope *
        (Math.sin(omega1 * t) * (v0 + zeta * omega0 * x0) / omega1 +
          x0 * Math.cos(omega1 * t)) -
      envelope *
        (Math.cos(omega1 * t) * (v0 + zeta * omega0 * x0) -
          omega1 * x0 * Math.sin(omega1 * t));
  } else {
    // Critically damped
    const envelope = Math.exp(-omega0 * t);
    position = config.toValue - envelope * (x0 + (v0 + omega0 * x0) * t);
    velocity =
      envelope * (v0 * (t * omega0 - 1) + t * x0 * (omega0 * omega0));
  }

  // updates
  state.time = now;
  state.velocity = velocity;
  state.position = position;
  state.frameTime = frameTime + deltaTime;

  // Conditions for stopping the spring animation
  let isOvershooting = false;
  if (config.overshootClamping && config.stiffness !== 0) {
    if (lastPosition < config.toValue) {
      isOvershooting = position > config.toValue;
    } else {
      isOvershooting = position < config.toValue;
    }
  }
  const isVelocity = Math.abs(velocity) <= config.restSpeedThreshold;
  let isDisplacement = true;
  if (config.stiffness !== 0) {
    isDisplacement =
      Math.abs(config.toValue - position) <= config.restDisplacementThreshold;
  }

  if (isOvershooting || (isVelocity && isDisplacement)) {
    if (config.stiffness !== 0) {
      // Ensure that we end up with a round value
      state.velocity = 0;
      state.position = config.toValue;
    }

    state.finished = true;
  }
}

class SpringNode extends AnimatedWithInput {
  _state;
  _config;

  constructor(state, config) {
    super([clock]);
    this._state = state;
    this._config = config;
  }

  update() {
    this.__getValue();
  }

  __onEvaluate() {
    spring(this._state, this._config);
  }
}

module.exports = SpringNode;
