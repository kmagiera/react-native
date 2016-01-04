/**
 * @providesModule ExpensiveListView
 * @flow
 */
'use strict';

import React, {
  Image,
  ListView,
  Platform,
  StyleSheet,
  PropTypes,
  Text,
  TouchableHighlight,
  View,
} from 'react-native';

import burnCPU from 'burnCPU';
const STORIES = require('./reactnative.json').data.children;

const SELF_IMAGE = "https://s3-us-west-2.amazonaws.com/examples-exp/reddit/self.png";
const DEFAULT_IMAGE = "https://s3-us-west-2.amazonaws.com/examples-exp/reddit/default.png";

function getImageUri(thumbnail) {
  if (thumbnail.match('http:')) {
    return { uri: thumbnail };
  } else if (thumbnail === 'self') {
    return { uri: SELF_IMAGE };
  } else {
    return { uri: DEFAULT_IMAGE };
  }
}

export default class ExpensiveListView extends React.Component {
  static propTypes = {
    onPressStory: PropTypes.func.isRequired,
  }

  constructor(props) {
    super(props);

    let dataSource = new ListView.DataSource({
      rowHasChanged: (r1, r2) => r1 !== r2,
    });

    dataSource = dataSource.cloneWithRows(STORIES);

    this.state = {
      dataSource,
    };
  }

  render() {
    return (
      <View style={styles.container}>
        <ListView
          initialListSize={1}
          pageSize={1}
          dataSource={this.state.dataSource}
          renderRow={this._renderRow.bind(this)}
        />
      </View>
    );
  }

  _renderRow({data}, rowId) {
    burnCPU(17 * 4); // Feel free to comment this out

    return (
      <TouchableHighlight
        onPress={() => { this._handlePress(data) }}
        underlayColor='rgba(0,0,0,0.03)'>
        <View style={styles.row}>
          <Image source={getImageUri(data.thumbnail)} style={styles.thumbnail} resizeMode="contain" />

          <View style={styles.rowInfo}>
            <Text style={styles.titleText}>{data.title}</Text>
            <Text style={styles.authorText}>Submitted by {data.author}</Text>
            <Text style={styles.commentText}>{data.num_comments} {parseInt(data.num_comments, 10) === 1 ? 'comment' : 'comments'}</Text>
          </View>
        </View>
      </TouchableHighlight>
    );
  }

  _handlePress({permalink}) {
    this.props.onPressStory(permalink);
  }
}


const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  row: {
    flexDirection: 'row',
    paddingTop: 15,
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
  },
  rowInfo: {
    flexDirection: 'column',
    paddingRight: 20,
    paddingLeft: 10,
    flexWrap: 'wrap',
    flex: 1,
  },
  titleText: {
    fontSize: 17,
    color: '#05a5d1',
  },
  authorText: {
    color: '#888',
    fontSize: 14,
    marginTop: 5,
  },
  commentText: {
    color: '#888',
    fontSize: 14,
  },
  thumbnail: {
    width: 70,
    height: 52.5,
    marginLeft: 5,
  },
});
