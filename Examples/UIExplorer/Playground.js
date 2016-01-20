/**
 * @providesModule Playground
 */
'use strict';

var React = require('react-native');
var {
  Animated,
  AppRegistry,
  BackAndroid,
  Dimensions,
  DrawerLayoutAndroid,
  Easing,
  PanResponder,
  processColor,
  StyleSheet,
  Text,
  ToolbarAndroid,
  View,
} = React;
var UIExplorerButton = require('./UIExplorerButton');

var FadeInView = React.createClass({
  getInitialState: function() {
    // var anim = new Animated.Value(0);
    // var index = 0;
    // var moved = anim.interpolate({
    //   inputRange: [index - 1, index, index + 1],
    //   outputRange: [0, 1, 0]
    // });// new Animated.multiply(anim, new Animated.Value(0.1));
    return {
      // fadeAnim: new Animated.Value(0), // opacity 0
      fadeAnim: new Animated.NativeValue(0), // opacity 0
      someAnim: new Animated.NativeValue(0),
      // vectorAnim: new Animated.ValueXY(),
      // springAnim: anim,//new Animated.Value(0),
      // moved: moved,
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
        duration: 2500,    // Configuration
      },
    ).start();             // Don't forget start!
    // Animated.decay(       // Uses easing functions
    //   this.state.fadeAnim, // The value to drive
    //   {
    //     velocity: 0.1,
    //   },
    // ).start();
    // Animated.timing(
    //   this.state.vectorAnim,
    //   {toValue: {x: 100, y: 200}},
    // ).start();
    // Animated.spring(
    //   this.state.springAnim,
    //   {
    //     toValue: 1,
    //   }
    // ).start();
  },

  render: function() {
    return (
      <Animated.View   // Special animatable View
        style={{
          // transform: [   // Array order matters
          //   {translateX: this.state.moved},
          // ],
          // transform: [   // Array order matters
          //   {translateX: this.state.vectorAnim.x,
          //    translateY: this.state.vectorAnim.y},
          // ],
          // opacity: this.state.moved,  // Binds
          opacity: this.state.fadeAnim,
          transform: [   // Array order matters
            {translateX: this.state.someAnim.interpolate({
              inputRange: [0, 1],
              outputRange: [0, 100],
            })},
          ],
          // width: Animated.multiply(this.state.someAnim, new Animated.Value(0.5, true)).interpolate({
          //   inputRange: [0, 1],
          //   outputRange: [200, 100],
          // })
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


var CIRCLE_SIZE = 80;
var CIRCLE_COLOR = 'blue';
var CIRCLE_HIGHLIGHT_COLOR = 'green';

var evt

var PanResponderExample = React.createClass({

  statics: {
    title: 'PanResponder Sample',
    description: 'Shows the use of PanResponder to provide basic gesture handling.',
  },

  _panResponder: {},
  _previousLeft: 0,
  _previousTop: 0,
  _circleStyles: {},
  _someAnim: new Animated.NativeValue(0),
  circle: (null : ?{ setNativeProps(props: Object): void }),

  componentWillMount: function() {
    this._panResponder = PanResponder.create({
      onStartShouldSetPanResponder: this._handleStartShouldSetPanResponder,
      onMoveShouldSetPanResponder: this._handleMoveShouldSetPanResponder,
      onPanResponderGrant: this._handlePanResponderGrant,
      // onPanResponderMove: this._handlePanResponderMove,
      onPanResponderMove: Animated.event(
        [{nativeEvent: {pageX: this._someAnim}}]
      ),
      onPanResponderRelease: this._handlePanResponderEnd,
      onPanResponderTerminate: this._handlePanResponderEnd,
    });
    this._previousLeft = 20;
    this._previousTop = 84;
    this._circleStyles = {
      style: {
        left: this._previousLeft,
        top: this._previousTop
      }
    };
  },

  componentDidMount: function() {
    this._updatePosition();
  },

  render: function() {
    return (
      <View
        style={styles.container}>
        <View
          ref={(circle) => {
            this.circle = circle;
          }}
          style={styles.circle}
          {...this._panResponder.panHandlers}
        />
        <Animated.View   // Special animatable View
        style={{
          // opacity: this._someAnim.interpolate({
          //   inputRange: [0, 500],
          //   outputRange: [1, 0.02],
          // }),
          transform: [   // Array order matters
            {scaleX: this._someAnim.interpolate({
              inputRange: [0, 400],
              outputRange: [1, 0.2],
            })},
          ],
        }}>
        <View style={styles.content}>
          <Text>Some other view</Text>
        </View>
      </Animated.View>
      </View>
    );
  },

  _highlight: function() {
    const circle = this.circle;
    circle && circle.setNativeProps({
      style: {
        backgroundColor: processColor(CIRCLE_HIGHLIGHT_COLOR)
      }
    });
  },

  _unHighlight: function() {
    const circle = this.circle;
    circle && circle.setNativeProps({
      style: {
        backgroundColor: processColor(CIRCLE_COLOR)
      }
    });
  },

  _updatePosition: function() {
    this.circle && this.circle.setNativeProps(this._circleStyles);
  },

  _handleStartShouldSetPanResponder: function(e: Object, gestureState: Object): boolean {
    // Should we become active when the user presses down on the circle?
    return true;
  },

  _handleMoveShouldSetPanResponder: function(e: Object, gestureState: Object): boolean {
    // Should we become active when the user moves a touch over the circle?
    return true;
  },

  _handlePanResponderGrant: function(e: Object, gestureState: Object) {
    this._highlight();
  },
  _handlePanResponderMove: function(e: Object, gestureState: Object) {
    this._circleStyles.style.left = this._previousLeft + gestureState.dx;
    this._circleStyles.style.top = this._previousTop + gestureState.dy;
    this._updatePosition();
  },
  _handlePanResponderEnd: function(e: Object, gestureState: Object) {
    this._unHighlight();
    this._previousLeft += gestureState.dx;
    this._previousTop += gestureState.dy;
  },
});

module.exports = PanResponderExample;

var Playground = React.createClass({
  render: function() {
    return (
      <View>
        <Text>Hello</Text>
        <FadeInExample/>
        <PanResponderExample/>
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
  circle: {
    width: CIRCLE_SIZE,
    height: CIRCLE_SIZE,
    borderRadius: CIRCLE_SIZE / 2,
    backgroundColor: CIRCLE_COLOR,
    position: 'absolute',
    left: 0,
    top: 0,
  },
});

module.exports = Playground;
