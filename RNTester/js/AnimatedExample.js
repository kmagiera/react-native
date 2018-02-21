/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @flow
 * @providesModule AnimatedExample
 */
'use strict';

var React = require('react');
var ReactNative = require('react-native');
var {
  Animated,
  Easing,
  StyleSheet,
  Text,
  View,
} = ReactNative;
var RNTesterButton = require('./RNTesterButton');

exports.framework = 'React';
exports.title = 'Animated - Examples';
exports.description = 'Animated provides a powerful ' +
  'and easy-to-use API for building modern, ' +
  'interactive user experiences.';

class FadeInView extends React.Component<$FlowFixMeProps, any> {
  constructor(props) {
    super(props);
    const firstAnim = new Animated.Value(100);
    const secondAnim = new Animated.Value(-50);
    // const sum = Animated.add(firstAnim, secondAnim)
    // const transX = Animated.cond(Animated.lessThan(sum, 25), sum, 10)

    // const stash = new Animated.Value(0);
    // const prev = new Animated.Value(0);
    // const diff = Animated.block([
    //   Animated.set(stash, Animated.add(firstAnim, Animated.multiply(-1, prev))),
    //   Animated.set(prev, firstAnim),
    //   stash
    // ])
    const diff = Animated.diff(firstAnim);

    const acc = Animated.acc(diff);

    const diffClamp = Animated.diffClamp(firstAnim, 0, 20);

    Animated.onChange(acc, Animated.call([acc], ([v]) => {
      console.log("ANIM", v)
    })).__attach();


    this.state = {
      firstAnim,
      secondAnim,
      // transX,
      acc,
      diff,
      diffClamp,
    }
  }
  componentDidMount() {
    // this._animation = Animated.timing(       // Uses easing functions
    //   this.state.firstAnim, // The value to drive
    //   {
    //     toValue: 0,        // Target
    //     duration: 2000,    // Configuration
    //   },
    // );
    this._animation = Animated.spring(this.state.firstAnim, {
      toValue: 0,
      // tension: 20,
      // friction: 0.5
    });
    this._animation.start();             // Don't forget start!
    // Animated.timing(this.state.secondAnim, { toValue: 0, duration: 5000}).start()
  }
  stop() {
    this._animation.stop();
  }
  render() {
    return (
      <Animated.View   // Special animatable View
        style={{
          // opacity: Animated.add(this.state.fadeAnim, this.state.offset)
          transform: [
            { translateX: Animated.add(0, this.state.diffClamp) }
          ]
        }}>
        {this.props.children}
      </Animated.View>
    );
  }
}

class FadeInExample extends React.Component<$FlowFixMeProps, any> {
  constructor(props) {
    super(props);
    this.state = {
      show: true,
    };
  }
  render() {
    return (
      <View>
        <RNTesterButton onPress={() => {
            // this.setState((state) => (
              // {show: !state.show}
            // ));
            this._ref.stop();
          }}>
          Press to {this.state.show ?
            'Hide' : 'Show'}
        </RNTesterButton>
        {this.state.show && <FadeInView ref={r => this._ref = r}>
          <View style={styles.content}>
            <Text>FadeInView</Text>
          </View>
        </FadeInView>}
      </View>
    );
  }
}

exports.examples = [
  {
    title: 'FadeInView',
    description: 'Uses a simple timing animation to ' +
      'bring opacity from 0 to 1 when the component ' +
      'mounts.',
    render: function() {

      return <FadeInExample />;
    },
  },
];

var styles = StyleSheet.create({
  content: {
    backgroundColor: 'deepskyblue',
    borderWidth: 1,
    borderColor: 'dodgerblue',
    padding: 20,
    margin: 20,
    borderRadius: 10,
    alignItems: 'center',
  },
});
