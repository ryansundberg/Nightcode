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

(defn create-window
  ([] (create-window nil))
  ([repl-handle]
    (let [console (editors/create-console "clj")
          pane (repl/create-pane console)
          frame (s/frame :title "Nightrepl"
                     :content pane
                     :on-close (if repl-handle :nothing :exit) ; can not close break point windows
                     :size [800 :by 600])]
      (repl/start-pane pane console repl-handle #(s/dispose! frame))
      (doto frame
        ; set various window properties
        window/enable-full-screen!
        window/add-listener!))))
  
(defn spawn-repl-window
  [repl-fn]
  (s/invoke-later
    (s/show! (create-window repl-fn))))

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
  (reset! redl/spawn-repl-window spawn-repl-window)
  (s/invoke-later
    (s/show! (reset! ui/root (create-window)))))
