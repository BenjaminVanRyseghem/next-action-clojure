(ns next-action.sync
  (:require [next-action.todoist :as todoist]))

(def todoist-tree)

(defn update
  "Updates the in memory data then chase the @next-action"
  []
  (println (todoist/sync-todoist))
  )
