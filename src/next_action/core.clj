(ns next-action.core
  (:require [next-action.info :as info]
            [next-action.sync :as sync])
  (:gen-class))

;; Run every minute
(def sleeping-time (* 1000 10 1 ))

(defn- infinite-loop
  "Main loop run every `sleeping-time`"
  [function]
  (function)
  (future (infinite-loop function))
  nil)

(defn- update
  "Loop body. Wait for `sleeping-time` then update data"
  []
  (do
    (println "\n=========== New Iteration ===========\n")
    (sync/update)
    (println "Now sleeping for" (quot sleeping-time 1000) "seconds" )
    (Thread/sleep sleeping-time)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (infinite-loop update))
