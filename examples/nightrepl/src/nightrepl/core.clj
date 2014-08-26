(ns nightrepl.core
  (:require [nightcode.customizations :as custom]
            [nightcode.editors :as editors]
            [nightrepl.repl :as repl]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.window :as window]
            [nightrepl.redl :as redl]
            [nightrepl.debug :as debug]
            [seesaw.core :as s])
  (:gen-class))

(defn create-root-window
  []
  (let [console (editors/create-console "clj")
        pane (repl/create-pane console)
        frame (s/frame :title "Nightrepl"
                       :content pane
                       :on-close :exit ; can not close break point windows
                       :size [800 :by 600])]
    (repl/start-pane pane console nil #(s/dispose! frame))
    (doto frame
      ; set various window properties
      window/enable-full-screen!
      window/add-listener!)))

(defn- create-stack-pane
  [thread-context]
  (let [stack-trace-text
        (binding [*out* (java.io.StringWriter.)]
          (doseq [ste (:stack-trace thread-context)]
            (clojure.stacktrace/print-trace-element ste)
            (println))
          (str *out*))]
    (s/text
      :text stack-trace-text
      :multi-line? true
      :editable? false
      :rows 6)))

(defn create-break-window
  [repl-handle thread-context]
  (let [console (editors/create-console "clj")
        stack-pane (create-stack-pane thread-context)
        console-pane (repl/create-pane console)
        frame (s/frame :title "Nightrepl"
                       :content (s/top-bottom-split (s/scrollable stack-pane) console-pane)
                       :on-close :nothing ; can not close break point windows
                       :size [800 :by 600])]
    (repl/start-pane console-pane console repl-handle #(s/dispose! frame))
    (doto frame
      ; set various window properties
      window/enable-full-screen!
      window/add-listener!)
    [frame stack-pane console-pane]))
  
(defn spawn-break-window
  [repl-handle thread-context]
  (s/invoke-later
    (do
      (let [[root stack console] (create-break-window repl-handle thread-context)]
        (s/show! root)
        (s/scroll! stack :to :top)))))

(defn -main [& args]
  ; listen for keys while modifier is down
  (shortcuts/listen-for-shortcuts!
    (fn [key-code]
      (case key-code
        ; Q
        81 (window/confirm-exit-app!)
        ; else
        false)))
  ; this will give us a nice dark theme by default, or allow a lighter theme
  ; by adding "-s light" to the command line invocation
  (window/set-theme! (custom/parse-args args))
  ; create and display the window
  ; it's important to save the window in the ui/root atom
  (reset! redl/spawn-repl-window spawn-break-window)
  (s/invoke-later
    (s/show! (reset! ui/root (create-root-window)))))
