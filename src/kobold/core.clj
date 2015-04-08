(ns kobold.core
  (:require [me.raynes.conch :refer [with-programs]]
            [clojure.edn :as edn]
            [alandipert.enduro :refer [atom* IDurableBackend]]))

(defn git
  ([args] (git args ""))
  ([args in]
   (if (not (sequential? args))
     (git [args] in)
     (with-programs [git]
       (let [repo "/home/paulus/prg/kobold/rep"]
         (-> (apply git (concat (map name args) [{:dir repo}]))
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

(declare ser-vec)
(declare ser-map)
(declare write-coll)

(defn ser-coll
  [x]
  (cond (vector? x) (ser-vec x)
        (map? x) (ser-map x)
        :default (throw (IllegalArgumentException. "Don't know how to serialize this"))))

(defn rf-str [hsh]
  (str "#kobold.core/rf \"" hsh "\"\n"))

(defn tree-str [hsh idx]
  (str "040000 tree " hsh "\t" idx "\n"))

(defn ser-vec-step
  [[out-str out-tree tree-count] e]
  (if (coll? e)
    (let [hsh (write-coll e)]
      [(str out-str (rf-str hsh))
       (str out-tree (tree-str hsh tree-count))
       (inc tree-count)])
    [(str out-str (prn-str e)) out-tree tree-count]))

(defn ser-vec
  "Returns a pair [blob tree-str]"
  [v]
  (let [[blob tree-str] (reduce ser-vec-step ["" "" 0] v)]
    [(str "[\n" blob "]\n") tree-str]))

(defn ser-map-step
  [[out-str out-tree tree-count] [k v]]
  (let [[kk vv] (map (fn [x] (if (coll? x)
                               (let [hsh (write-coll x)] [(rf-str hsh) hsh])
                               [(pr-str x) nil]))
                     [k v])
        tree-entries (keep second [kk vv])]
    [(str out-str (first kk) " " (first vv) "\n")
     (apply str
            out-tree
            (map-indexed (fn [idx te] (tree-str te (+ tree-count idx))) tree-entries))
     (+ tree-count (count tree-entries))]))

(defn ser-map
  "Returns a pair [blob tree-str]"
  [v]
  (let [[blob tree-str] (reduce ser-map-step ["" "" 0] v)]
    [(str "{\n" blob "}\n") tree-str]))

(defn coll->tree
  [v]
  "Converts a collection into a string representation of a git tree object"
  (let [[blob tree-str] (ser-coll v)
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
  (git [:update-ref "refs/heads/master" commit-hash])
  (git [:symbolic-ref "HEAD" "refs/heads/master"])
  (git [:reset "--hard"]))

(defn commit-coll
  [v]
  (let [hd (read-head)]
    (-> v write-coll (commit-tree hd) advance-head)))

(defn show-head
  []
  (-> (read-head) read-coll))

(deftype GitBackend [bla]
  IDurableBackend
  (-commit!
    [this value]
    (println "commit!")
    true)
  (-remove!
    [this]
    (println "remove!")))

(defn git-atom
  [init]
  (atom* init (GitBackend. nil) {}))
