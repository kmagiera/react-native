/**
 * @providesModule ExpensiveDetailView
 */
'use strict';

import React, {
  ListView,
  Platform,
  PropTypes,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import burnCPU from 'burnCPU';
const IS_NESTED_COMMENT = 'this-is-a-nested-comment';

export default class DetailScreen extends React.Component {

  static propTypes = {
    url: PropTypes.string.isRequired,
  }

  constructor(props) {
    super(props);

    this._loadInitialDataAsync = this._loadInitialDataAsync.bind(this);
    this._renderRow = this._renderRow.bind(this);

    let dataSource = new ListView.DataSource({
      rowHasChanged: (r1, r2) => r1 !== r2,
    });

    this.state = {
      dataSource,
      story: null,
    };
  }

  componentWillMount() {
    this._loadInitialDataAsync();
  }

  render() {
    return (
      <View style={styles.container}>
        {this._renderStory()}
        {this._renderNoComments()}
        <ListView
          style={{flex: 1}}
          dataSource={this.state.dataSource}
          renderRow={this._renderRow} />
      </View>
    );
  }

  _renderStory() {
    let { story } = this.state;

    if (!story) {
      return;
    }

    return (
      <View style={styles.storyContainer}>
        <Text style={styles.storyTitleText}>
          {story.title}
        </Text>
        <Text style={styles.storyAuthorText}>
          {story.author}
        </Text>
      </View>
    );
  }

  _renderNoComments() {
    let { story, dataSource } = this.state;

    if (!story || dataSource.getRowCount() > 0) {
      return;
    }

    return (
      <View style={styles.noCommentsContainer}>
        <Text style={styles.noCommentsText}>
          No comments on this post yet
        </Text>
      </View>
    );
  }

  async _loadInitialDataAsync() {
    let response = await fetch(`https://www.reddit.com/${this.props.url}.json`);
    let json = await response.json();
    this._onReady(json);
  }

  _onReady([{data: {children: [{data: story}]}}, {data: {children: comments}}]) {
    let dataSource = this.state.dataSource.cloneWithRows(comments);
    this.setState({story, dataSource});
  }

  _renderRow({data}, additionalInfo) {
    burnCPU(17 * 4); // Feel free to comment this out

    let replies;

    if (data.replies !== '') {
      replies = data.replies.data.children;
    }

    return (
      <View key={data.id} style={[
        styles.commentContainer,
        additionalInfo === IS_NESTED_COMMENT && styles.nestedCommentContainer
       ]}>
        <Text style={styles.commentAuthorText}>{data.author}</Text>
        <Text style={styles.commentBodyText}>{data.body}</Text>
        { replies &&
          replies.map(row => this._renderRow(row, IS_NESTED_COMMENT)) }
      </View>
    );
  }

}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  storyTitleText: {
    fontSize: 18,
  },
  storyAuthorText: {
    color: '#888',
  },
  storyContainer: {
    paddingHorizontal: 5,
    paddingVertical: 15,
    paddingTop: 10,
    margin: 5,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
  },
  commentContainer: {
    margin: 10,
  },
  commentAuthorText: {
    fontSize: 14,
    color: '#888',
  },
  nestedCommentContainer: {
    backgroundColor: '#f9f9f9',
    padding: 10,
    marginBottom: 0,
    marginTop: 15,
  },
  commentBodyText: {
    marginTop: 3,
  },
  noCommentsContainer: {
    flex: 1,
    alignItems: 'center',
    paddingTop: 50,
  },
  noCommentsText: {
    color: '#888',
  },
});
