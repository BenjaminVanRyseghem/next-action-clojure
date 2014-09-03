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
    (and (not ignored) (not is-parallel) valid-parent)))

(defn- add-label-patch [task]
  {:type "item_update"
   :timestamp (quot (System/currentTimeMillis) 1000)
   :args {:id (:id task)
          :name (:content task)
          :labels (conj (:labels task) (todoist/get-next-action-id))}})

(defn- remove-label-patch [task]
  {:type "item_update"
   :timestamp (quot (System/currentTimeMillis) 1000)
   :args {:id (:id task)
          :name (:content task)
          :labels (remove #(= (todoist/get-next-action-id) %1) (:labels task))}})

(defn- contains-next-action-label? [task]
  (not (nil? (some #{(todoist/get-next-action-id)} (:labels task)))))

(defn- attach-sequential-patches [patches project]
  (let [first-task (first @(:tasks project))]
;;      (println (:content first-task)"'s LABELS:" (:labels first-task) (todoist/get-next-action-id) (contains-next-action-label? first-task))
    (if-not (or (nil? first-task) (contains-next-action-label? first-task))
      (swap! patches conj (add-label-patch first-task))))
  (doall
   (for [task (rest @(:tasks project))]
     (if (contains? (:labels task) (todoist/get-next-action-id))
       (swap! patches conj (remove-label-patch task))))))

(defn- attach-parallel-patches [patches project]
  (let [mapping (fn [task] (add-label-patch task))]
    (doall
     (for [task @(:tasks project)]
       (if-not (contains-next-action-label? task)
         (swap! patches conj (mapping task)))))))

(defn- attach-ignored-patches [patches project]
  (for [task @(:tasks project)]
     (if (contains? (:labels task) (todoist/get-next-action-id))
       (swap! patches conj (remove-label-patch task)))))

(defn collect-patches [projects]
  (let [patches (atom [])]
    (doall
     (for [project projects]
       (cond
        (sequential-project? project) (attach-sequential-patches patches project)
        (parallel-project? project) (attach-parallel-patches patches project)
        (is-ignored project) (attach-ignored-patches patches project))))
    @patches))
