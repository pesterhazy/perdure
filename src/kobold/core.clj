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
  (git [:hash-object "-w" "--stdin"] st))

(defn mktree [tree]
  (git :mktree tree))

(defn cat-file
  [rev]
  (println (git [:cat-file "-p" rev])))

;; (declare wrt)

;; (defn ser
  ;;   "Serializes the argument"
;;   [x]
;;   (if (vector? x)
;;     (reduce (fn [[outstr hashes] el] (let [[st hsh]])[(str outstr st "\n") (conj hashes hsh)]) ["" #{}] x)
;;     [(pr-str x) nil]))

;; (defn wrt
;;   [x]
;;   (if (vector? x)
;;     (let [hsh (hash-object (ser x))]
;;       [(str "#rf \"" hsh  "\"") hsh])
;;     [(ser x) nil]))


(defn ser-vec [v]
  (-> (map prn-str v)
      clojure.string/join))

(defn vec->tree
  [v]
  "Converts a vector into a string representation of a git tree object"
  (let [serialized (ser-vec v)
        hsh (hash-object serialized)]
    (str "100644 blob " hsh "\troot")))

(defn write-vec [v]
  (-> v vec->tree mktree))

(defn read-vec [hsh]
  (git [:show (str hsh ":root")]))
