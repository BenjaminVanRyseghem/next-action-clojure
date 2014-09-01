(ns next-action.sync
  (:require [next-action.nextaction :as next-action]
            [next-action.todoist :as todoist]))

(def todoist-tree)

(defn update
  "Updates the in memory data then chase the @next-action"
  []
  (let [result (todoist/get-projects)
        projects (:projects result)
        patch (:patch result)
        new-patches (next-action/collect-patches projects)]
    (println "Avant -> " (count patch))
    (println "Apres -> " (count @new-patches))
    (println "Final -> " (dec (count (conj @new-patches patch))))))
