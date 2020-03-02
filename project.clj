(defproject rbh "0.1.1"
  :description "Redbubble Helper"
  :license {:name "ISC License"
            :url "https://github.com/johanthoren/redbubble-helper"
            :year 2020
            :key "isc"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.6.0"]
                 [clj-commons/fs "1.5.0"]
                 [clj-logging-config "1.9.12"]
                 [simple-progress "0.1.3"]
                 [trptcolin/versioneer "0.2.0"]]
  :main rbh.core
  :target-path "target/%s"
  ;; :aliases
  ;; {"native"
  ;;  ["shell"
  ;;   "native-image"
  ;;   "--initialize-at-build-time"
  ;;   "--no-fallback"
  ;;   ;; "-H:+PrintClassInitialization"
  ;;   ;; "-H:+TraceClassInitialization"
  ;;   "-H:ReflectionConfigurationFiles=resources/META-INF/native-image/reflect-config.json"
  ;;   "-H:+ReportExceptionStackTraces"
  ;;   "-jar" "./target/uberjar/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
  ;;   "-H:Name=target/${:name}"]}
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-shell "0.5.0"]]
  :bin {:name "rbh"
        :bin-path "~/bin/"})
