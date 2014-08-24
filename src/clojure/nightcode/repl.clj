(ns nightcode.repl
  (:require [nightcode.editors :as editors]
            [nightcode.lein :as lein]
            [nightcode.sandbox :as sandbox]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as utils]
            [nightcode.debug :as debug]
            [seesaw.core :as s]))

(defn run-repl!
  "Starts a REPL process."
  [process in-out]
  (lein/stop-process! process)
  #_(->> (if (sandbox/get-dir)
         (clojure.main/repl)
         (lein/start-process-indirectly! process nil "clojure.main"))
       (lein/start-thread! in-out))
  (lein/start-thread! in-out debug/repl))

(defn create-pane
  "Returns the pane with the REPL."
  [console]
  (let [process (atom nil)
        run! (fn [& _]
               (s/request-focus! (-> console .getViewport .getView))
               (run-repl! process (ui/get-io! console)))
        pane (s/config! console :id :repl-console)]
    (utils/set-accessible-name! (.getTextArea pane) :repl-console)
    ; start the repl
    (run!)
    ; create a shortcut to restart the repl
    (when-not (sandbox/get-dir)
      (shortcuts/create-hints! pane)
      (shortcuts/create-mappings! pane {:repl-console run!}))
    ; return the repl pane
    pane))
