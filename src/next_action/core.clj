(ns next-action.core
  (:require [next-action.info :as info]
            [next-action.sync :as sync]))

;; Run every minute
(def ^{:private true} sleeping-time (* 1000 (info/sleeping-time) ))

(defn- human-readable-date []
  (.format (java.text.SimpleDateFormat. "MM/dd/yyyy HH:mm:ss") (java.util.Date.)))

(defn update
  "Loop. Update data then wait for `sleeping-time`."
  []
  (do
    (println "\n=========== New Iteration [" (human-readable-date) "] ===========\n")
    (sync/update)
    (println "Now sleeping for" (quot sleeping-time 1000) "seconds" )
    (Thread/sleep sleeping-time)
    (recur)))

(defn -main
  "Main function. Just run the infinite loop for updates."
  [& args]
  (update))
