(ns next-action.nextaction
  (:require [next-action.info :as info]
            [next-action.todoist :as todoist]))

(defn- ignored? [project]
  (let [name (:name project)]
    (or
     (.startsWith name (info/someday-label))
     (.startsWith name (info/list-prefix))
     (and
      (not (nil? @(:parent project)))
      (ignored? @(:parent project))))))

(defn- parallel-project? [project]
  (.endsWith (:name project) (info/parallel-postfix)))

(defn- sequential-project? [project]
  (let [ignored (ignored? project)
        is-parallel (parallel-project? project)]
    (and
     (not ignored)
     (not is-parallel))))

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

;;
;; Sequential
;;

(declare attach-sequential-patches!)

(defn- add-new-patch! [patches patch]
  (swap! patches conj patch))

(defn- remove-patches-for-sequential-task! [patches task]
  (if (contains-next-action-label? task)
    (add-new-patch! patches (remove-label-patch task)))
  (doall
   (for [child @(:children task)]
     (remove-patches-for-sequential-task! patches task))))

(defn- add-patches-for-sequential-task! [patches task]
  (if (empty? @(:children task))
    (if-not (contains-next-action-label? task)
      (add-new-patch! patches (add-label-patch task)))
    (do
      (if (contains-next-action-label? task)
        (add-new-patch! patches (remove-label-patch task)))
      (attach-sequential-patches! patches @(:children task)))))

(defn- attach-sequential-patches! [patches coll]
  (if-not (empty? coll)
    (let [first-task (first coll)]
      (if-not (nil? first-task)
        (add-patches-for-sequential-task! patches first-task)))
    (doall
     (for [task (rest coll)]
       (remove-patches-for-sequential-task! patches task)))))

;;
;; Parallel
;;

(defn- attach-parallel-patches! [patches coll]
  (doseq [task coll]
    (if (empty? @(:children task))
      (if-not (contains-next-action-label? task)
        (add-new-patch! patches (add-label-patch task)))
      (do
        (if (contains-next-action-label? task)
          (add-new-patch! patches (remove-label-patch task)))
        (attach-parallel-patches! patches @(:children task))))))

;;
;; Ignored
;;

(defn- attach-ignored-patches! [patches coll]
  (doseq [task coll]
    (do
      (if (contains-next-action-label? task)
        (do
          (println "Remove task")
          (add-new-patch! patches (remove-label-patch task))))
      (attach-ignored-patches! patches @(:children task)))))

;;
;; Core
;;

(defn collect-patches
  "Iterates over all the projects and items to collect the patches"
  [projects]
  (let [patches (atom [])]
    (doseq [project projects]
      (do
        (cond
         (ignored? project) (attach-ignored-patches! patches @(:tasks project))
         (sequential-project? project) (attach-sequential-patches! patches (filter #(= 1 (:indent %1)) @(:tasks project)))
         (parallel-project? project) (attach-parallel-patches! patches @(:tasks project)))))
    @patches))
