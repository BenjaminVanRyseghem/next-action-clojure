(ns next-action.todoist
  (:require [next-action.info :as info]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]))

(def todoist-url "https://todoist.com/TodoistSync/v5.3/get")
(def seq-no (atom 0))

(defn sync-todoist
  []
  (let [options
        (codec/form-encode
         {:api_token (info/api-token)
          :seq_no seq-no})
        url (str todoist-url "?" options)
        json (get (client/get url) :body)
        body (json/read-str json :key-fn keyword)]

    ;; updates seq-no
    (println url)
    (reset! seq-no (:seq_no body))
    json
    ))
