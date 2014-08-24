(ns nightrepl.repl
  (:require [nightcode.editors :as editors]
            [nightcode.lein :as lein]
            [nightcode.sandbox :as sandbox]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as utils]
            [nightrepl.debug :as debug]
            [seesaw.core :as s]))

(defn run-repl!
  "Starts a REPL process."
  [process in-out repl-handle end-callback]
  (lein/stop-process! process)
  (lein/start-thread! in-out 
                      (if repl-handle
                        (debug/run-repl repl-handle)
                        (debug/repl))
                      (end-callback)))

(defn create-pane
  "Returns the pane with the REPL."
  [console]
  (let [pane (s/config! console :id :repl-console)]
    (utils/set-accessible-name! (.getTextArea pane) :repl-console)
    ; return the repl pane
    pane))

(defn start-pane
  [pane console repl-handle repl-terminate-callback]
  (let [process (atom nil)
        run! (fn [& _]
               (s/request-focus! (-> console .getViewport .getView))
               (run-repl! process (ui/get-io! console) repl-handle repl-terminate-callback))]
    ; start the repl
    (run!)
    ; create a shortcut to restart the repl
    (when-not (sandbox/get-dir)
      (shortcuts/create-hints! pane)
      (shortcuts/create-mappings! pane {:repl-console run!}))))