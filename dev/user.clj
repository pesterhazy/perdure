(ns user
  (:require [pesterhazy.perdure :refer :all]
            [alandipert.enduro :as e]
            [clojure.java.io :as io]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]))

(defn rt
  []
  (test/run-tests 'pesterhazy.perdure-test))
