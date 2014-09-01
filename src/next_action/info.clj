(ns next-action.info
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def read-info
  (memoize
   #(json/read-str
     (slurp (io/file (-> (java.io.File. "info.json") .getAbsolutePath)))
     :key-fn keyword)))

(defn api-token []
  (:api-token (read-info)))

(defn someday-label []
  (:someday-label (read-info)))

(defn list-prefix []
  (:list-prefix (read-info)))

(defn next-action-label []
  (:next-action-label (read-info)))

(defn parallel-postfix []
  (:parallel-postfix (read-info)))
