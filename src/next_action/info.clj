(ns next-action.info
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))


(defn- read-info []
  (json/read-str
   (slurp (io/file (-> (java.io.File. "info.json") .getAbsolutePath)))
   :key-fn keyword))

(defn api-token []
  (:api-token (read-info)))
