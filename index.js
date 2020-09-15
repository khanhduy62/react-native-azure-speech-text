import {
  NativeModules,
  NativeEventEmitter,
} from "react-native";

const { AzureSpeechText } = NativeModules;

const AzureSpeechTextEmitter = new NativeEventEmitter(AzureSpeechText);

export default class RNAzureSpeechText {

  static config(params) {
    AzureSpeechText.config(params);
  }

  static async speechToText() {
    await AzureSpeechText.speechToText();
  }

  static async textToSpeech(text, voiceName) {
    await AzureSpeechText.textToSpeech(text, voiceName);
  }

  static addListener(eventName, callback) {
    AzureSpeechTextEmitter.addListener(eventName, callback);
  }
}

