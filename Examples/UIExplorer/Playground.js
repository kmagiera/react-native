/**
 * @providesModule Playground
 */
'use strict';

var React = require('react-native');
var {
  AppRegistry,
  BackAndroid,
  Dimensions,
  DrawerLayoutAndroid,
  StyleSheet,
  ToolbarAndroid,
  View,
  Text,
  Animated,
  Easing,
} = React;
var UIExplorerButton = require('./UIExplorerButton');

var FadeInView = React.createClass({
  getInitialState: function() {
    return {
      fadeAnim: new Animated.Value(0), // opacity 0
      someAnim: new Animated.Value(0),
    }
  },

  componentDidMount: function() {
    Animated.timing(       // Uses easing functions
      this.state.fadeAnim, // The value to drive
      {
        toValue: 1,        // Target
        duration: 2000,    // Configuration
      },
    ).start();             // Don't forget start!
    Animated.timing(       // Uses easing functions
      this.state.someAnim, // The value to drive
      {
        toValue: 1,        // Target
        duration: 2000,    // Configuration
      },
    ).start();             // Don't forget start!
  },

  render: function() {
    return (
      <Animated.View   // Special animatable View
        style={{
          opacity: this.state.fadeAnim,  // Binds
          transform: [   // Array order matters
            {translateX: this.state.someAnim.interpolate({
              inputRange: [0, 1],
              outputRange: [0, 100],
            })},
          ],
          width: this.state.someAnim.interpolate({
            inputRange: [0, 1],
            outputRange: [200, 100],
          })
        }}>
        {this.props.children}
      </Animated.View>
    );
  }
});

var FadeInExample = React.createClass({

  getInitialState: function() {
    return {
      show: true,
    }
  },

  render: function() {
    return (
      <View>
        <UIExplorerButton onPress={() => {
            this.setState((state) => (
              {show: !state.show}
            ));
          }}>
          Press to {this.state.show ?
            'Hide' : 'Show'}
        </UIExplorerButton>
        {this.state.show && <FadeInView>
          <View style={styles.content}>
            <Text>FadeInView</Text>
          </View>
        </FadeInView>}
      </View>
    );
  }
});

var Playground = React.createClass({
  render: function() {
    return (
      <View>
        <Text>Hello</Text>
        <FadeInExample/>
      </View>
    )
  },
});

var styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  toolbar: {
    backgroundColor: '#E9EAED',
    height: 56,
  },
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

module.exports = Playground;
