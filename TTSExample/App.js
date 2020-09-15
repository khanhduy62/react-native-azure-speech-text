
import React from 'react';

import {
  View,
  TouchableOpacity,
  Text,
  NativeModules,
  NativeEventEmitter,
} from 'react-native';



const { TextToSpeechEdge } = NativeModules;

const calendarManagerEmitter = new NativeEventEmitter(TextToSpeechEdge);

export default class App extends React.Component {
  constructor(props) {
    super(props);
    this.ttedgeFinish = null;
    this.ttedgeStart = null;

  }

  componentDidMount() {
    TextToSpeechEdge.config({
      subscription: '<YOUR_SUB_KEY>',
      region: '<YOUR_REGION>',
    });


    this.ttedgeFinish = calendarManagerEmitter.addListener('ttedge-finish',
      () => {
        console.log('ttedge-finish');
      });
    this.ttedgeStart = calendarManagerEmitter.addListener('tts-start',
      () => {
        console.log('tts-start');
      });
  }

  componentWillUnmount() {
    this.ttedgeFinish && this.ttedgeFinish.remove();
  }

  start = async () => {
    try {
      const text = await TextToSpeechEdge.createSpeechToText();
      if (text) {
        await TextToSpeechEdge.createTextToSpeechByText(text,
          'en-US-AriaNeural');
      } else {

      }
    } catch (error) {
      console.log('[ERROR]', error);
    }
  };

  render() {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <TouchableOpacity onPress={this.start}>
          <Text>Start</Text>
        </TouchableOpacity>
      </View>
    );
  }
}
