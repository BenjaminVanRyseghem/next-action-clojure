(ns next-action.nextaction
  (:require [next-action.info :as info]
            [next-action.todoist :as todoist]))

(defn- is-ignored [project]
  (let [name (:name project)]
    (or (.startsWith name (info/someday-label)) (.startsWith name (info/list-prefix)))))

(defn- parallel-project? [project]
  (.endsWith (:name project) (info/parallel-postfix)))

(defn- sequential-project? [project]
  (let [ignored (is-ignored project)
        valid-parent (or (nil? @(:parent project)) (not (is-ignored @(:parent project))))
        is-parallel (parallel-project? project)]

;;     (println (:name project)
;;              "->\n\t\tignored:" ignored
;;              "\n\t\tvalid-parent:" valid-parent
;;              "\n\t\tparent:" @(:parent project)
;;              "\n\t\tis-parallel" is-parallel)
    (and (not ignored) (not is-parallel) valid-parent)))

(defn- attach-sequential-patches [patches project]
  (let [first-task (first @(:tasks project))]
    (if-not (or (nil? first-task) (contains? (:labels first-task) (todoist/get-next-action-id)))
      (swap! patches conj {
                           :type "item_update"
                           :name (:content first-task)
                           :timestamp (quot (System/currentTimeMillis) 1000)
                           :args {
                                  :id (:id first-task)
                                  :labels (conj (:labels first-task) (todoist/get-next-action-id))
                                  }}))))

(defn- attach-parallel-patches [patches project]
  (let [mapping (fn [task] {
                            :type "item_update"
                            :name (:content task)
                            :timestamp (quot (System/currentTimeMillis) 1000)
                            :args {
                                   :id (:id task)
                                   :labels (conj (:labels task) (todoist/get-next-action-id))
                                   }})]
    (doall
     (for [task @(:tasks project)]
         (swap! patches conj (mapping task))))))

(defn collect-patches [projects]
  (let [patches (atom [])]
    (doall
     (for [project projects]
       (cond
        (sequential-project? project) (attach-sequential-patches patches project)
        (parallel-project? project) (attach-parallel-patches patches project))))
    patches))
