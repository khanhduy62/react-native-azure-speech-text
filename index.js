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
    let text = await AzureSpeechText.speechToText();
    if (text) {
      text = text.replace(".", "");
      text = text.toLowerCase();
    }
    return text;
  }

  static async textToSpeech(text, voiceName = "en-US-AriaNeural") {
    await AzureSpeechText.textToSpeech(text, voiceName);
  }

  static addListener(eventName, callback) {
    AzureSpeechTextEmitter.addListener(eventName, callback);
  }

  static stopSpeechToText() {
    AzureSpeechText.stopSpeechToText();
  }

  static stopTextToSpeech() {
    AzureSpeechText.stopTextToSpeech();
  }
}

