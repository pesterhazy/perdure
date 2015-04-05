(ns kobold.core
  (:require [me.raynes.conch :refer [with-programs]]))

(defn git
  ([args] (git args ""))
  ([args in]
   (if (not (sequential? args))
     (git [args] in)
     (with-programs [git]
       (let [repo ["--git-dir" "/home/paulus/prg/kobold/rep/.git"]]
         (-> (apply git (concat repo (map name args) [{:in in}]))
             (clojure.string/trim-newline)))))))

(defn hash-object
  "Takes a string, writes it to the object store. Returns hash"
  [st]
  (git [:hash-object "--stdin"] st))

(defn cat-file
  [rev]
  (println (git [:cat-file "-p" rev])))

(declare wrt)

(defn ser
  "Serializes the argument"
  [x]
  (if (vector? x)
    (let [result (with-out-str (doseq [e (map wrt x)]
                                 (println e)))]
      result)
    (pr-str x)))

(defn wrt
  [x]
  (if (vector? x)
    (str "#rf \"" (hash-object (ser x)) "\"")
    (ser x)))
