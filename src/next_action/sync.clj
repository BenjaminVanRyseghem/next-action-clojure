(ns next-action.sync
  (:require [next-action.nextaction :as next-action]
            [next-action.todoist :as todoist]))

(defn update
  "Updates the in memory data then chase the @next-action"
  []
  (let [result (todoist/get-projects)
        projects (:projects result)
        patch (:patch result)]
    (if (nil? patch)
      (let [patches (next-action/collect-patches projects)]

        (println (count patches) "update(s)")
        (doall
         (for [p patches]
           (println "\t" (:type p) "\t" (get-in p [:args :name]))))
        (if (empty? patches)
          (println "\t No update needed" )
          (do
            (todoist/send-patches patches)
            (println "\t Task(s) updated sucessfully"))))
      (do
        (println "\tCreation of the new label")
        (todoist/send-patches (list patch))
        (println "\tLabel created successfully")))))
