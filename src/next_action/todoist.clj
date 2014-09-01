(ns next-action.todoist
  (:require [next-action.info :as info]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]))

(def todoist-url "https://todoist.com/TodoistSync/v5.3/get")
(def seq-no (atom 0))
(def next-action-id (atom nil))

(defn- get-todoist []
  (let [options
        (codec/form-encode
         {:api_token (info/api-token)
          ;; todo: should use @seq-no, and update in memory data
          :seq_no 0})
        url (str todoist-url "?" options)
        json (get (client/get url) :body)
        body (json/read-str json :key-fn keyword)]
    (reset! seq-no (:seq_no body))
    body))

(defn- sibling-case [project parent]
  (reset! (:parent project) @parent))

(defn- children-case [project parent last-project indent]
  (reset! parent @last-project)
  (sibling-case project parent)
  (swap! indent inc))

(defn- parent-case [project parent indent]
  (reset! parent @(:parent @parent))
  (sibling-case project parent)
  (swap! indent dec))

(defn- build-projects-tree [projects]
  (let [sorted (map #(assoc %1 :parent (atom nil) :tasks (atom [])) (sort-by :item_order projects))
        parent (atom nil)
        result (atom {})
        last-project (atom nil)
        indent (atom 1)]
    (doall
     (for [project (rest sorted)]
       (do
         (cond
          (= @indent (:indent project)) (sibling-case project parent)
          (= (inc @indent) (:indent project)) (children-case project parent last-project indent)
          (< (:indent project) @indent) (parent-case project parent indent))
         (reset! last-project project)
         (swap! result assoc (:id project) project))))
    @result))

(defn- attach-tasks [tasks projects]
  (doall
   (for [task tasks]
     (let [project (get projects (:project_id task))]
       (swap! (:tasks project) conj task)))))

(defn- retrieve-next-action-id [data]
  (let [labels (:Labels data)
        label (first (filter #(= (:name %1) (info/next-action-label)) labels))]
    (if (nil? label)
      (let [uuid (str (java.util.UUID/randomUUID))]
        (reset! next-action-id uuid)
        {
         :type "label_register"
         :timestamp (quot (System/currentTimeMillis) 1000)
         :temp_id uuid
         :args {
                :name (info/next-action-label)
                :color 5
                }})
      (do
        (reset! next-action-id (:id label))
        {}))))

(defn get-next-action-id [] @next-action-id)

(defn get-projects
  "Retrieve all the projects from Todoist, then build the projects hierarchy
  and attach each task to its project."
  []
  (let [data (get-todoist)
        projects (:Projects data)
        result (build-projects-tree projects)
        patch (retrieve-next-action-id data)]
    (attach-tasks (:Items data) result)
    (doall
     (for [project (vals result)]
       (reset! (:tasks project)
               (sort-by :item_order @(:tasks project)))))
    {:projects (vals result) :patch patch}))
