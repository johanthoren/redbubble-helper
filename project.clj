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
  :aliases
  {"deb"
   ["shell"
    "bash"
    "build-deb.sh"
    "redbubble-helper"
    "imagemagick (>= 8:6.9.2.10-dfsg-2~), inkscape (>= 0.92.4-4), openjdk-8-jre-headless"
    "Redbubble Helper"
    "rbh"
    "A simple CLI tool to generate images suitable for Redbubble."]}

  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-redline-rpm "0.4.3"]
            [lein-shell "0.5.0"]]
  :bin {:name "rbh"}
  :rpm {:package-name "redbubble-helper"
        :distribution "Redbubble Helper"
        :summary "A simple CLI tool to generate images suitable for Redbubble."
        :vendor "Johan Thorén <johan@thoren.xyz>"
        :packager "Johan Thorén <johan@thoren.xyz>"
        :release "1"
        :provides "rbh"
        :requires [["ImageMagick" "<<" "7"]
                   ["inkscape" ">=" "0.92.2"]
                   ["java-11-openjdk-headless" ">=" "11"]]
        :files [["target/default/rbh" "/usr/local/bin/rbh" 0755 0750 "root" "root"]]})
