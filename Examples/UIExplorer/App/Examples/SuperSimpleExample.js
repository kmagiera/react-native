/**
 * Copyright 2004-present Facebook. All Rights Reserved.
 *
 * @providesModule SuperSimpleExample
 * @flow
 */
'use strict';

var React = require('react-native');
var {
  Alert,
  Animated,
  View,
  Text,
  StyleSheet,
} = React;

import burnCPU from 'burnCPU';

var NavigationBasicExample = React.createClass({

  getInitialState() {
    return {
      opacity: new Animated.Value(0),
    }
  },

  componentDidMount() {
    Animated.timing(this.state.opacity, {toValue: 1, duration: 5000}).start();
    requestAnimationFrame(() => {
      burnCPU(3000);
      // This causes a little flicker when the original animation completes!
      Animated.spring(this.state.opacity, {toValue: 0}).start(() => {
        Alert.alert('completion fn not invoked!');
        Alert.alert('original animation also not canceled! so this flashes back to the end result of the timing anim');
        // Animated.spring(this.state.opacity, {toValue: 1}).start();
      });
    });
  },

  render() {
    let { opacity } = this.state;

    return (
      <View style={styles.container}>
        <Animated.View
          style={{
            opacity,
          }}>
          <View style={{width: 200, height: 200, backgroundColor: 'red',}} />
        </Animated.View>
      </View>
    );
  },
});

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});

module.exports = NavigationBasicExample;
