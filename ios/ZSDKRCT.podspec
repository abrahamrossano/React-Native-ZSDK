Pod::Spec.new do |s|
  s.name         = "ZSDKRCT"
  s.version      = "0.1.0"
  s.summary      = "Bridge Zebra Link-OS para React Native"
  s.license      = { :type => "MIT" }
  s.author       = { "CDigital" => "sistemas@cdigital.com.mx" }
  s.homepage     = "https://github.com/abrahamrossano/React-Native-ZSDK"
  s.platform     = :ios, "13.0"
  s.source       = { :path => "." }
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.dependency   "React-Core"
  # s.dependency "linkos" # si usas el pod oficial de Zebra, descomenta esta línea
end