/**
 * Copyright 2004-present Facebook. All Rights Reserved.
 *
 * @providesModule NavigationAnimatedExample
 * @flow
 */
'use strict';

var React = require('react-native');
var Navigation = require('../Navigation');
var {
  View,
  Text,
} = React;

class NavigationAnimatedExample extends React.Component {
  render() {
    return (
      <Navigation.RootContainer
        initialStack={new Navigation.Stack([ 'First Route' ], 0)}
        renderNavigator={(stack, onNavigation) => (
          <Navigation.AnimatedStackView
            stack={stack}
            style={{flex: 1}}
            renderOverlay={(props) => (
              <Navigation.HeaderView
                {...props}
                getTitle={route => route}
              />
            )}
            renderRoute={(props) => (
              <Navigation.CardView
                {...props }>
                <View style={{flex: 1, alignItems: 'center', justifyContent: 'center'}}>
                  <View style={{padding: 20, backgroundColor: '#ccc', borderRadius: 5}}>
                    <Text
                      style={{fontSize: 15}}
                      onPress={() => { onNavigation(new Navigation.Action.Push('Another Route')); }}>
                      Push!
                    </Text>
                  </View>
                </View>
              </Navigation.CardView>
            )}
          />
        )}
      />
    );
  }
}

module.exports = NavigationAnimatedExample;
