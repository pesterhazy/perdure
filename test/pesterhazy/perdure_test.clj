(ns pesterhazy.perdure-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [pesterhazy.perdure :refer :all]))

;; (def compound (fn [inner-gen]
;;                 (gen/one-of [(gen/vector inner-gen)
;;                              (gen/map inner-gen inner-gen)])))
;; (def scalars (gen/one-of [gen/int gen/boolean]))
;; (def my-json-like-thing (gen/recursive-gen compound scalars))

;; (defspec round-trip
;;   50
;;   (prop/for-all [v my-json-like-thing]
;;                 (= v (-> v write-coll read-coll)))) 
