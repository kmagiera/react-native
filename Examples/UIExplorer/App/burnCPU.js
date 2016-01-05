/**
 * @providesModule burnCPU
 * @flow
 */
'use strict';

let performanceNow = require('fbjs/lib/performanceNow');

export default function burnCPU(milliseconds) {
  const start = performanceNow();
  while (performanceNow() < (start + milliseconds)) {}
}
