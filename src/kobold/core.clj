(ns kobold.core
  (:require [me.raynes.conch :refer [with-programs]]
            [clojure.edn :as edn]))

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

(defn nest? [e] (or (map? e) (vector? e)))

(declare ser-vec)
(declare ser-map)
(declare write-coll)

(defn ser
  [x]
  (cond (vector? x) (ser-vec x)
        (map? x) (ser-map x)
        :default (prn-str x)))

(defn ser-vec
  "Returns a pair [blob tree-str]"
  [v]
  (let [[blob tree-str] (reduce
                         (fn [[out-str out-tree tree-count] e]
                           (if (nest? e)
                             (let [hsh (write-coll e)]
                               [(str out-str (str "#kobold.core/rf \"" hsh "\"\n"))
                                (str out-tree "040000 tree " hsh "\t" tree-count "\n")
                                (inc tree-count)])
                             [(str out-str (prn-str e)) out-tree tree-count]))
                         ["" "" 0]
                         v)]
    [(str "[\n" blob "]\n") tree-str]))

(defn ser-map-step
  [[out-str out-tree tree-count] [k v]]
  (let [[kk vv] (map (fn [x] (if (nest? x)
                               (let [hsh (write-coll x)]
                                 [(str "#kobold.core/rf \"" hsh "\"") hsh])
                               [(pr-str x) nil]))
                     [k v])
        tree-entries (keep second [kk vv])]
    [(str out-str (first kk) " " (first vv) "\n")
     (apply str
            out-tree
            (map-indexed (fn [idx te]
                           (str "040000 tree " te "\t" (+ tree-count idx) "\n"))
                         tree-entries))
     (+ tree-count (count tree-entries))]))

(defn ser-map
  "Returns a pair [blob tree-str]"
  [v]
  (let [[blob tree-str] (reduce ser-map-step ["" "" 0] v)]
    [(str "{\n" blob "}\n") tree-str]))

(defn coll->tree
  [v]
  "Converts a collection into a string representation of a git tree object"
  (let [[blob tree-str] (ser v)
        hsh (hash-object blob)]
    (str "100644 blob " hsh "\troot\n" tree-str)))

(defn write-coll
  "Writes a collection to the db. Returns hash of the tree object"
  [v]
  (-> v coll->tree mktree))

;; ----

(declare read-coll)

(defn read-blob [hsh]
  (git [:show (str hsh ":root")]))

(defn blob->coll
  [st]
  (edn/read-string {:readers {'kobold.core/rf read-coll}} st))

(defn read-coll
  "Takes the hash of a tree object and reads the collection stored"
  [hsh]
  (-> hsh read-blob blob->coll))

;; ---

(defn read-head
  "Returns the hash of the commit pointed to by HEAD"
  []
  (git [:rev-parse "HEAD"]))

(defn commit-tree
  [tree-hash parent-hash]
  (git [:commit-tree "-m" "Kobold commit" "-p" parent-hash tree-hash]))

(defn advance-head
  "Advances `head` to the specified commit"
  [commit-hash]
  (git [:update-ref "refs/heads/master" commit-hash]))
