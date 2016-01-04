/**
 * Copyright 2004-present Facebook. All Rights Reserved.
 *
 * @providesModule NavigationJankyExample
 * @flow
 */
'use strict';

import React, {
  View,
  Text,
  TouchableOpacity,
} from 'react-native';

import ExpensiveListView from 'ExpensiveListView';
import ExpensiveDetailView from 'ExpensiveDetailView';
const Navigation = require('../Navigation');

class NavigationAnimatedExample extends React.Component {
  render() {
    return (
      <Navigation.RootContainer
        initialStack={new Navigation.Stack([ {id: 'root', title: 'Hello!'} ], 0)}
        renderNavigator={(stack, onNavigation) => (
          <Navigation.AnimatedStackView
            stack={stack}
            renderRoute={(props) => this._renderRoute(props, onNavigation)}
            style={{flex: 1}}
            renderOverlay={(props) => (
              <Navigation.HeaderView
                {...props}
                getTitle={route => route.title}
              />
            )}
          />
        )}
      />
    );
  }

  _renderRoute(props, onNavigation) {
    let { route } = props;

    if (route.id === 'root') {
      return (
        <Navigation.CardView
          {...props }>
          <View style={{flex: 1, alignItems: 'center', justifyContent: 'center'}}>
            <TouchableOpacity
              style={{padding: 20, backgroundColor: '#ccc', borderRadius: 5}}
              onPress={() => { onNavigation(new Navigation.Action.Push({id: 'list', title: '/r/reactnative'})); }}>
              <Text style={{fontSize: 15}}>Go to /r/reactnative!</Text>
            </TouchableOpacity>
          </View>
        </Navigation.CardView>
      )
    } else if (route.id === 'list') {
      return (
        <Navigation.CardView
          {...props }>
          <ExpensiveListView
            onPressStory={(url) => { onNavigation(new Navigation.Action.Push({id: 'detail', title: 'Detail', url})); }}
          />
        </Navigation.CardView>
      )
    } else if (route.id === 'detail') {
      return (
        <Navigation.CardView
          {...props }>
          <ExpensiveDetailView url={route.url} />
        </Navigation.CardView>
      );
    }
  }
}

module.exports = NavigationAnimatedExample;
