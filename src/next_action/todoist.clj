(ns next-action.todoist
  (:require [next-action.info :as info]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]))

(def todoist-url "https://todoist.com/TodoistSync/v5.3/get")
(def seq-no (atom 0))

(defn- get-todoist
  []
  (let [options
        (codec/form-encode
         {:api_token (info/api-token)
          ;; todo: should use @seq-no, and update in memory data
          :seq_no 0})
        url (str todoist-url "?" options)
        json (get (client/get url) :body)
        body (json/read-str json :key-fn keyword)]

    ;; updates seq-no
    (reset! seq-no (:seq_no body))
    body))

(defn- sibling-case [project parent]
  (println "sibling-case")
  (reset! (:parent project) @parent))

(defn- children-case [project parent last-project indent]
  (println "children-case")
  (reset! parent @last-project)
  (reset! (:parent project) @parent)
  (swap! indent inc))

(defn- parent-case [project parent indent]
  (println "parent-case")
  (swap! parent :parent)
  (reset! (:parent project) @parent)
  (swap! indent dec))

(defn get-projects
  []
  (let [data (get-todoist)
        projects (:Projects data)
        sorted (map #(assoc %1 :parent (atom nil)) (sort-by :item_order projects))
        parent (atom nil)
        last-project (atom nil)
        indent (atom 1)]
    (doall (for [project (rest sorted)]
      (do
        (cond
         (= @indent (:indent project)) (sibling-case project parent)
         (= (inc @indent) (:indent project)) (children-case project parent last-project indent)
         (< (:indent project) @indent) (parent-case project parent indent))
        (reset! last-project project))))
    sorted))
