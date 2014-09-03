(ns next-action.todoist
  (:require [next-action.info :as info]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]))

(def get-todoist-url "https://todoist.com/TodoistSync/v5.3/get")
(def sync-todoist-url "https://todoist.com/TodoistSync/v5.3/sync")

(def seq-no (atom 0))
(def next-action-id (atom nil))

(defn- generate-temp-id []
  (str (java.util.UUID/randomUUID))
  )

(defn- get-todoist []
  (let [options (codec/form-encode
                 {:api_token (info/api-token)
                  ;; todo: should use @seq-no, and update in memory data
                  :seq_no 0})
        url (str get-todoist-url "?" options)
        json (get (client/get url) :body)
        body (json/read-str json :key-fn keyword)]
    ;;     (reset! seq-no (:seq_no body))
    body))

;;
;; Tasks tree building
;;

(defn- sibling-task! [task parent]
  (reset! (:parent task) @parent)
  (if-not (nil? @parent)
    (swap! (:children @parent) conj task )))

(defn- children-task! [task parent last-task indent]
  (reset! parent @last-task)
  (sibling-task! task parent)
  (swap! indent inc))

(defn- parent-task! [task parent indent]
  (reset! parent @(:parent @parent))
  (sibling-task! task parent)
  (swap! indent dec))

(defn- build-tasks-tree! [tasks]
  (let [sorted (map #(assoc %1 :parent (atom nil) :children (atom [])) (filter #(and (zero? (:is_archived %1)) (zero? (:is_deleted %1)) (zero? (:checked %1))) (sort-by :item_order tasks)))
        parent (atom nil)
        last-task (atom nil)
        indent (atom 1)]
    (doall
     (for [task sorted]
       (do
         (cond
          (= @indent (:indent task)) (sibling-task! task parent)
          (= (inc @indent) (:indent task)) (children-task! task parent last-task indent)
          (< (:indent task) @indent) (parent-task! task parent indent))
         (reset! last-task task))))
    sorted))

;; No need to sort the tasks by priority or due date since:
;; - in sequential projects, you still have to do them in the correct order
;; - in parallell projects, all the tasks are flagged as **next-action**
(defn- sort-tasks! [tasks]
  (reset! tasks (build-tasks-tree! @tasks)))

;;
;; Project tree building
;;

(defn- sibling-project! [project parent]
  (reset! (:parent project) @parent))

(defn- children-project! [project parent last-project indent]
  (reset! parent @last-project)
  (sibling-project! project parent)
  (swap! indent inc))

(defn- parent-project! [project parent indent]
  (reset! parent @(:parent @parent))
  (sibling-project! project parent)
  (swap! indent dec))

(defn- build-projects-tree! [projects]
  (let [sorted (map #(assoc %1 :parent (atom nil) :tasks (atom [])) (sort-by :item_order projects))
        parent (atom nil)
        result (atom {})
        last-project (atom nil)
        indent (atom 1)]
    (doall
     (for [project (rest sorted)]
       (do
         (cond
          (= @indent (:indent project)) (sibling-project! project parent)
          (= (inc @indent) (:indent project)) (children-project! project parent last-project indent)
          (< (:indent project) @indent) (parent-project! project parent indent))
         (reset! last-project project)
         (swap! result assoc (:id project) project))))
    @result))

;;
;;
;;

(defn- attach-tasks [tasks projects]
  (doall
   (for [task tasks]
     (let [project (get projects (:project_id task))]
       (swap! (:tasks project) conj task)))))

(defn- retrieve-next-action [data]
  (let [labels (:Labels data)
        label (first (filter #(= (:name %1) (info/next-action-label)) labels))]
    (if (nil? label)
      (let [uuid (generate-temp-id)]
        (reset! next-action-id uuid)
        {
         :type "label_register"
         :timestamp (quot (System/currentTimeMillis) 1000)
         :temp_id uuid
         :args {
                :name (info/next-action-label)
                :color 2 ;; red
                }})
      (do
        (reset! next-action-id (:id label))
        nil))))


;;
;; CORE
;;

(defn get-next-action-id
  "Returns the id for the **next-action** label.
  Note that it can be a temporary id."
  []
  @next-action-id)

(defn get-projects
  "Retrieve all the projects from Todoist, then build the projects hierarchy
  and attach each task to its project."
  []
  (let [data (get-todoist)
        projects (:Projects data)
        result (build-projects-tree! projects)
        patch (retrieve-next-action data)]
    (attach-tasks (:Items data) result)
    (doall
     (for [project (vals result)]
       (sort-tasks! (:tasks project))))
    {:projects (vals result) :patch patch}))


(defn send-patches
  "Send the patches to Todoist to update data"
  [patches]
  (let [options (codec/form-encode
                 {:api_token (info/api-token)
                  :items_to_sync (json/write-str patches)})
        url (str sync-todoist-url "?" options)
        ]
    (client/get url)
    "Task flagged successfully"))
