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

(defn ser
  "Serializes the argument"
  [x]
  (if (vector? x)
    (let [st (->> x
                  (map ser)
                  (clojure.string/join))]
      (print st))
    (prn-str x)))
