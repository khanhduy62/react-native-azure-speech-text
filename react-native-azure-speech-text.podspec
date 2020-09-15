require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-azure-speech-text"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                 react-native-azure-speech-text
                   DESC
  s.homepage     = "https://github.com/phithu/react-native-azure-speech-text"
  # brief license entry:
  s.license      = "MIT"
  # optional - use expanded license entry instead:
  # s.license    = { :type => "MIT", :file => "LICENSE" }
  s.authors      = { "Anderson Neves" => "atsneves@gmail.com" }
  s.platforms    = { :ios => "9.3" }
  s.source       = { :git => "https://github.com/phithu/react-native-azure-speech-text.git", :tag => "#{s.version}" }
  s.ios.deployment_target  = '9.3'

  s.source_files = "ios/**/*.{h,c,m}"
  s.requires_arc = true

  s.framework = 'AVFoundation'
  s.dependency "React"
  s.dependency "MicrosoftCognitiveServicesSpeech-iOS"
end

