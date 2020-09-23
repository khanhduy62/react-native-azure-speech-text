#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTLog.h>
#import "TextToSpeechEdge.h"
#import <MicrosoftCognitiveServicesSpeech/SPXSpeechApi.h>

@implementation TextToSpeechEdge {
    NSString * sub;
    NSString * region;
    NSString * ignoreSilentSwitch;
}

RCT_EXPORT_MODULE(AzureSpeechText)

RCT_EXPORT_METHOD(config:(NSDictionary *)params) {
    sub = params[@"subscription"];
    region = params[@"region"];
    ignoreSilentSwitch = [params objectForKey:@"ignoreSilentSwitch"] ? params[@"ignoreSilentSwitch"]: @"ignore";
}

-(NSArray<NSString *> *)supportedEvents
{
    return @[@"tts-start", @"ttedge-finish", @"tts-pause", @"tts-resume", @"tts-progress", @"tts-cancel"];
}

RCT_EXPORT_METHOD(stopTextToSpeech)
{
    [_player stop];
}

RCT_EXPORT_METHOD(stopSpeechToText)
{
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryAmbient error:nil];
}

RCT_EXPORT_METHOD(textToSpeech:(NSString *)text withVoiceName:(nonnull NSString *)voiceName resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    SPXSpeechConfiguration *speechConfig = [[SPXSpeechConfiguration alloc] initWithSubscription:sub region:region];
    if (!speechConfig) {
        NSString * code = @"SPXSpeechConfiguration_Failure";
        NSString * message = @"Could not load speech config";
        NSError * error  = [
                            NSError errorWithDomain:@"Could not load speech config"
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@"Could not load speech config"}
                            ];
        reject(code, message, error);
    }

    [speechConfig setSpeechSynthesisVoiceName:[voiceName isEqualToString:@""] ? @"en-US-AriaNeural" : voiceName];
    SPXSpeechSynthesizer *speechSynthesizer = [[SPXSpeechSynthesizer alloc] initWithSpeechConfiguration:speechConfig audioConfiguration:nil];

    SPXSpeechSynthesisResult *speechResult = [speechSynthesizer speakText:text];

    if (SPXResultReason_Canceled == speechResult.reason) {
        SPXSpeechSynthesisCancellationDetails *details = [[SPXSpeechSynthesisCancellationDetails alloc] initFromCanceledSynthesisResult:speechResult];
        NSLog(@"Speech synthesis was canceled: %@. Did you pass the correct key/region combination?", details.errorDetails);
        NSString * code = @"SPXResultReason_Canceled";
        NSString * message = @"Speech synthesis was canceled: %@. Did you pass the correct key/region combination?";
        NSError * error  = [
                            NSError errorWithDomain:@"Speech synthesis was canceled: %@. Did you pass the correct key/region combination?"
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@"Speech synthesis was canceled: %@. Did you pass the correct key/region combination?"}
                            ];
        reject(code, message, error);
    } else if (SPXResultReason_SynthesizingAudioCompleted == speechResult.reason) {
        if([ignoreSilentSwitch isEqualToString:@"ignore"]) {
            [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:nil];
        } else if([ignoreSilentSwitch isEqualToString:@"obey"]) {
            [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryAmbient error:nil];
        }

        _player = [[AVAudioPlayer alloc] initWithData:[speechResult audioData] error:nil];
        _player.delegate = self;
        [_player prepareToPlay];
        [_player play];

        resolve(@(YES));
    } else {
        NSString * code = @"ToSpeech_Error";
        NSString * message = @"There was an error to speech.";
        NSError * error  = [
                            NSError errorWithDomain:@"There was an error to speech."
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@"There was an error to speech."}
                            ];
        reject(code, message, error);
    }
}

RCT_REMAP_METHOD(speechToText,
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
    SPXSpeechConfiguration *speechConfig = [[SPXSpeechConfiguration alloc] initWithSubscription:sub region:region];
    if (!speechConfig) {
        NSString * code = @"SPXSpeechConfiguration_Failure";
        NSString * message = @"Could not load speech config";
        NSError * error  = [
                            NSError errorWithDomain:@"Could not load speech config"
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@"Could not load speech config"}
                            ];
        reject(code, message, error);
    }

    SPXSpeechRecognizer * speechRecognizer = [[SPXSpeechRecognizer alloc] init:speechConfig];
    if (!speechRecognizer) {
        NSString * code = @"Recognizer_Failure";
        NSString * message = @"Could not create speech recognizer";
        NSError * error  = [
                            NSError errorWithDomain:@"Could not create speech recognizer"
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@"Could not create speech recognizer"}
                            ];
        reject(code, message, error);
    }

    SPXSpeechRecognitionResult *speechResult = [speechRecognizer recognizeOnce];
    if (SPXResultReason_Canceled == speechResult.reason) {
        SPXCancellationDetails *details = [[SPXCancellationDetails alloc] initFromCanceledRecognitionResult:speechResult];
        NSLog(@"Speech recognition was canceled: %@. Did you pass the correct key/region combination?", details.errorDetails);

        NSString * code = @"SPXResultReason_Canceled";
        NSString * message = @"Speech recognition was canceled: %@. Did you pass the correct key/region combination?";
        NSError * error  = [
                            NSError errorWithDomain:@"Speech recognition was canceled: %@. Did you pass the correct key/region combination?"
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@""}
                            ];
        [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryAmbient error:nil];
        [speechRecognizer stopContinuousRecognition];
        reject(code, message, error);
    } else if (SPXResultReason_RecognizedSpeech == speechResult.reason) {
        [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryAmbient error:nil];
        [speechRecognizer stopContinuousRecognition];
        resolve(speechResult.text);
    } else {
        NSString * code = @"ToText_Error";
        NSString * message = @"There was an error to text.";
        NSError * error  = [
                            NSError errorWithDomain:@"There was an error to text."
                            code:500
                            userInfo:@{NSLocalizedDescriptionKey:@"There was an error to text."}
                            ];
        [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryAmbient error:nil];
        [speechRecognizer stopContinuousRecognition];
        reject(code, message, error);
    }
}

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag
{
    [self sendEventWithName:@"ttedge-finish" body:@{@"name": @"ttedge-finish"}];
}

@end
