(ns rbh.core
  (:require [me.raynes.fs :as fs]
            [clojure.java.shell :as shell]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j]
            [clojure.string :as str]
            [clojure.set :refer [difference]]
            [trptcolin.versioneer.core :refer [get-version]]
            [simple-progress.bar :as bar])
  (:gen-class))

(defn set-default-root-logger!
  [loglevel pattern]
  (clj-logging-config.log4j/set-loggers! :root
                                         {:level loglevel
                                          :pattern pattern
                                          :out :console}))

(set-default-root-logger! :info "%m%n")

(def exit-messages
  "Exit messages used by `exit`."
  {:64 ["ERROR 64: No input file specified."]})

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit
  "Print a `message` and exit the program with the given `status` code.
  See also [[exit-messages]]."
  [status message]
  (println message)
  (System/exit status))

(def version-number
  "The version number as defined in project.clj."
  (get-version "rbh" "rbh"))

(defn bash
  "Run `cmd` as an argument to the shell command 'bash -c'.
  This enables most shell syntax to be preserved and to work as expected."
  [cmd]
  (log/debug "Running the following command in bash:" cmd)
  (shell/sh "bash" "-c" cmd))

(defn svg?
  "Test if a `file` is a valid SVG file nor not.
  Uses the Unix/Linux CLI tool 'file'."
  [file]
  (if (= "SVG Scalable Vector Graphics image"
         (str/trim (:out (bash (str "file " file " | cut -d ':' -f 2")))))
    true
    false))

(defn png?
  "Test if a `file` is a valid PNG file nor not.
  Uses the Unix/Linux CLI tool 'file'."
  [file]
  (if (= "PNG image data"
         (str/trim (:out (bash (str "file " file
                                    " | cut -d ':' -f 2 | cut -d ',' -f 1")))))
    true
    false))

(defn png-dimensions?
  "Extract the dimensions of a `file`, which must be a PNG file.
  Uses the Unix/Linux CLI tool 'file'.
  See also [[png?]]."
  [file]
  {:pre [(png? file)]}
  (as-> (str "file " file " | cut -d ':' -f 2 | cut -d ',' -f 2") <>
    (bash <>)
    (:out <>)
    (str/trim <>)
    (str/split <> #"[\s]")
    (if (and (= "x" (nth <> 1)) (Integer. (first <>)) (Integer. (last <>)))
      {:width (Integer. (first <>))
       :height (Integer. (last <>))}
      (throw (Exception. "Unable to extract dimensions.")))))

(defn svg-dimensions?
  "Extract the dimensions of a `file`, which must be a SVG file.
  Uses the Unix/Linux CLI tool 'file'.
  See also [[svg?]]."
  [file]
  {:pre [(svg? file)]}
  (let [s (slurp file)
        width (Float. (last (re-find #"width=\"([\d]+[.]?[\d]*)" s)))
        height (Float. (last (re-find #"height=\"([\d]+[.]?[\d]*)" s)))]
    {:width width :height height}))

(defn greater-dimension?
  "Determine what dimension is the greater, width or height."
  [d]
  (let [r (compare (:width d) (:height d))]
    (condp = r
      -1 [:height]
      0 [:width :height]
      1 [:width])))

(defn extract-dimensions
  [s]
  (str/split (re-find #"[\d]+ x [\d]+" s) #"[\s]"))

(defn parse-inkscape-output
  [s]
  (as-> s <>
    (str/split-lines <>)
    {:background (last (str/split (first <>) #"[\s]"))
     :width (Integer. (first (extract-dimensions (nth <> 1))))
     :height (Integer. (last (extract-dimensions (nth <> 1))))
     :file (last (str/split (last <>) #"[\s]"))}))

(defn image-path [i] (first (str/split i (re-pattern (fs/base-name i)))))

(defn add-padding!
  [image]
  {:pre [(png? image)]}
  (let [cmd (bash (str "mogrify -bordercolor transparent -border 700 "
                       image))]
    (if (not-empty (:err cmd))
      (throw (Exception. (str "Failed to add padding to " image)))
      image)))

(defn generate-png!
  [input output & {:keys [width height] :or {width nil height nil}}]
  {:pre [(svg? input)
         (or (nil? width) (Integer. width))
         (or (nil? height) (Integer. height))]}
  (log/debug (str "Generating " output " from " input "."))
  (let [png (:out (bash (str "inkscape -z -e " output
                             (when width (str " -w " width))
                             (when height (str " -h " height))
                             " -D " input)))]
    (when width (log/debug "Target image width:" width))
    (when height (log/debug "Target image height:" height))
    (parse-inkscape-output png)))

(defn generate-big-png
  [input output]
  {:pre [(svg? input)]}
  (let [d (svg-dimensions? input)
        g (greater-dimension? d)]
    (condp = (count g)
      2 (generate-png! input output :width 12100 :height 12100)
      1 (generate-png! input output (first g) 12100))))

(defn generate-big-padded-png
  [input output]
  {:pre [(svg? input)]}
  (add-padding! (:file (generate-big-png input output))))

(defn replace-svg-color
  [image color]
  {:pre [(svg? image)
         (= 6 (count color))]}
  (str/replace (slurp image) #"fill:#[\d]{6}" (str "fill:#" color)))

(defn black-to-white-svg!
  [input output]
  (spit output (replace-svg-color input "ffffff")))

(defn rotate-png!
  [input output n]
  {:pre [(png? input)]}
  (bash (str "convert " input " -rotate " n " " output)))

(defn image-actions
  [image]
  {:pre [(svg? image)]}
  (let [path (image-path image)
        file-name (fs/name image)
        white-svg (str path "white-" file-name ".svg")
        large-png (str path file-name ".png")
        large-white-png (str path "white-" file-name ".png")
        rotated-90-png (str path file-name "-90.png")
        rotated-90-white-png (str path "white-" file-name "-90.png")
        rotated-270-png (str path file-name "-270.png")
        rotated-270-white-png (str path "white-" file-name "-270.png")
        tasks [#(black-to-white-svg! image white-svg)
               #(generate-big-padded-png image large-png)
               #(generate-big-padded-png white-svg large-white-png)
               #(rotate-png! large-png rotated-90-png 90)
               #(rotate-png! large-png rotated-270-png 270)
               #(rotate-png! large-white-png rotated-90-white-png 90)
               #(rotate-png! large-white-png rotated-270-white-png 270)]
        b (bar/mk-progress-bar (count tasks))]
    (log/info (str "Generating images based on " (fs/base-name image) ":"))
    (doseq [t tasks]
      (t)
      (b)))
  (println))

(def cli-options
  ;; First three strings describe a short-option, long-option with optional
  ;; example argument description, and a description. All three are optional
  ;; and positional.
  [["-d" "--debug" "Sets log level to debug" :default false]
   ["-h" "--help" "Print this help message" :default false]
   ["-v" "--version" "Print the current version number of rbh."
    :default false]])

(defn usage
  "Print a brief description and a short list of available options.
  See also [[cli-options]]."
  [options-summary]
  (log/debug "Printing the usage message.")
  (str/join
   \newline
   ["Generate images for Redbubble using an SVG file as input."
    ""
    "Usage: rbh [OPTIONS] file ..."
    ""
    "Options:"
    options-summary]))

(defn validate-args
  "Validate command line arguments.
  See also [[cli-options]]."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        files (map #(str (fs/absolute %)) arguments)
        non-existing (remove #(fs/exists? %) files)
        non-svg (remove #(svg? %) (difference (set files) (set non-existing)))]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      (:version options) ; version => exit OK with version number
      {:exit-message version-number :ok? true}
      (empty? files)
      (exit 64 (error-msg (:64 exit-messages)))
      (seq non-existing)
      (exit 65 (str "ERROR 65: One or more files do not exist:\n"
                    (str/join \newline non-existing)))
      (seq non-svg)
      (exit 66 (str "ERROR 66: One or more of the files are not"
                    " proper svg files:\n"
                    (str/join \newline non-svg)))
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      :else
      {:debug (:debug options)
       :files files})))

(defn -main [& args]
  (let [{:keys [debug files exit-message ok?]} (validate-args args)]
    (when debug
      (set-default-root-logger! :debug "%d [%p] %c (%t) %m%n")
      (log/debug "Debug logging enabled."))
    (when exit-message                          ; If a flag like '-h' was given,
      (exit (if ok? 0 1) exit-message))      ; then exit and show the message.
    (doseq [f files]
      (image-actions f))
    (System/exit 0)))
