(defproject perdure "0.1.0-SNAPSHOT"
  :description "Clojure's persistent data structures made durable"
  :url "http://github.com/pesterhazy/perdure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [me.raynes/conch "0.8.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]
                                  [org.clojure/test.check "0.7.0"]
                                  [alandipert/enduro "1.2.0"]]
                   :source-paths ["dev"]}})
