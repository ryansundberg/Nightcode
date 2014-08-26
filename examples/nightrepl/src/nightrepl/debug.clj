(ns nightrepl.debug
  (:require [nightcode.editors :as editors]
            [nightcode.lein :as lein]
            [nightcode.sandbox :as sandbox]
            [nightcode.shortcuts :as shortcuts]
            [nightcode.ui :as ui]
            [nightcode.utils :as utils]
            [nightrepl.redl :as redl]
            [clojure.core.async :as async]
            [seesaw.core :as s]))

(defn- wrap-form
  [form]
  (if (string? form)
    (str "\"" form "\"")
    (str form)))

(defn- debug-read
  [debug-repl request-prompt request-exit]
  (if (some? (deref (:state debug-repl)))
    (clojure.main/repl-read request-prompt request-exit)
    request-exit))

(defn- debug-eval
  [debug-repl form]
  (let [result (redl/repl-eval (:handle debug-repl) (wrap-form form))]
    (reset! (:state debug-repl) (when result (select-keys result [:ns :repl-depth])))
    (:out result)))

(defn- depth-prefix
  [depth]
  (if (> depth 0)
    (str "[debug-" depth "] ")
    ""))

(defn- debug-prompt
  [debug-repl]
  (let [state (deref (:state debug-repl))
        prefix (depth-prefix (:handle debug-repl))]
    (printf "%s%s=> " prefix (:ns state))))

(defn- debug-print
  [debug-repl value]
  (pr (symbol value)))

(defn- initialize-state
  [repl-handle]
  (redl/repl-eval repl-handle "(use '[nightrepl.redl :only [break continue]])")
  (let [result (redl/repl-eval repl-handle "nil")]
    (select-keys result [:ns :repl-depth])))

(defn run-repl
  [handle]
  {:pre [(some? handle)]}
  (let [debug-repl {:state (atom (initialize-state handle))
                    :handle handle}
        outer-repl (clojure.main/repl
                     :read (partial debug-read debug-repl)
                     :prompt (partial debug-prompt debug-repl)
                     :eval (partial debug-eval debug-repl)
                     :print (partial debug-print debug-repl))]
    outer-repl))

(defn repl
  "Start a debug repl"
  []
  (run-repl (redl/make-repl)))
