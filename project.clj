(defproject next-action "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/BenjaminVanRyseghem/next-action-clojure"
  :license {:name "General Public License v3.0"
            :url "http://www.gnu.org/copyleft/gpl.html"}
  :dependencies [[clj-http "1.0.0"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.5"]
                 [ring/ring-codec "1.0.0"]]
  :main ^:skip-aot next-action.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
