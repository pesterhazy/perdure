(ns kobold.core
  (:require [me.raynes.conch :refer [with-programs]]))

(defn git
  ([args] (git args nil))
  ([args in]
   (with-programs [git]
     (-> (apply git (concat args [{:in in}]))
         (clojure.string/trim-newline)))))

(defn hash-object
  "Takes a string, writes it to the object store. Returns hash"
  [st]
  (git ["hash-object" "--stdin"] st))

(defn ser
  "Serializes the argument"
  [x]
  (if (vector? x)
    (let [st (->> x
                  (map ser)
                  (clojure.string/join))]
      (print st))
    (prn-str x)))
