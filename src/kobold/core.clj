(ns kobold.core
  (:require [me.raynes.conch :refer [with-programs]]))

(defn to-obj
  "Takes a string, writes it to the object store. Returns hash"
  [st]
  (with-programs [git]
    (-> (git "hash-object" "--stdin"
             {:in st})
        (clojure.string/trim-newline))))

(defn ser
  "Serializes the argument"
  [x]
  (if (vector? x)
    (let [st (->> x
                  (map ser)
                  (clojure.string/join))]
      (print st))
    (prn-str x)))
