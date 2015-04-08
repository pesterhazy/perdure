(ns kobold.core
  (:require [me.raynes.conch :refer [with-programs]]
            [clojure.edn :as edn]
            [alandipert.enduro :refer [atom* IDurableBackend]]))

(defn git
  ([args] (git args ""))
  ([args in]
   (if (sequential? args)
     (with-programs [git]
       (let [repo "/home/paulus/prg/kobold/rep"
             ;;_ (println "git" (clojure.string/join " " (map pr-str args)))
             params (concat (map name args) [{:in in, :dir repo}])]
         (-> (apply git params)
             (clojure.string/trim-newline))))
     (git [args] in))))

(defn obj-type
  [hsh]
  (-> (git [:cat-file "-t" hsh])
      keyword))

(defn hash-object
  "Takes a string, writes it to the object store. Returns hash"
  [st]
  (git [:hash-object "-w" "--stdin"] st))

(defn mktree [tree]
  (git :mktree tree))

(defn cat-file
  [rev]
  (println (git [:cat-file "-p" rev])))

(defn commit-hash->tree-hash
  [commit-hash]
  (assert (= :commit (obj-type commit-hash)))
  (some->> (git [:cat-file "-p" commit-hash])
           (re-find #"(?m)^tree ([0-9a-f]*)$")
           second))

(declare ser-vec)
(declare ser-map)
(declare write-coll*)

(defn ser-coll
  [dict x]
  (cond (vector? x) (ser-vec dict x)
        (map? x) (ser-map dict x)
        :default (throw (IllegalArgumentException. "Don't know how to serialize this"))))

(defn rf-str [hsh]
  (str "#kobold.core/rf \"" hsh "\"\n"))

(defn tree-str [hsh idx]
  (str "040000 tree " hsh "\t" idx "\n"))

(defn ser-vec-step
  [dict [out-str out-tree tree-count] e]
  (if (coll? e)
    (let [hsh (write-coll* dict e)]
      [(str out-str (rf-str hsh))
       (str out-tree (tree-str hsh tree-count))
       (inc tree-count)])
    [(str out-str (prn-str e)) out-tree tree-count]))

(defn ser-vec
  "Returns a pair [blob tree-str]"
  [dict v]
  (let [[blob tree-str] (reduce (partial ser-vec-step dict) ["" "" 0] v)]
    [(str "[\n" blob "]\n") tree-str]))

(defn ser-map-step
  [dict [out-str out-tree tree-count] [k v]]
  (let [[kk vv] (map (fn [x] (if (coll? x)
                               (let [hsh (write-coll* dict x)] [(rf-str hsh) hsh])
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
  [dict v]
  (let [[blob tree-str] (reduce (partial ser-map-step dict) ["" "" 0] v)]
    [(str "{\n" blob "}\n") tree-str]))

(defn coll->tree
  [dict v]
  "Converts a collection into a string representation of a git tree object"
  (let [[blob tree-str] (ser-coll dict v)
        hsh (hash-object blob)]
    (str "100644 blob " hsh "\troot\n" tree-str)))

(defn write-coll*
  "Writes a collection to the db. Returns hash of the tree object"
  [dict v]
  (if-let [sha (get-in @dict [:obj->sha v])]
    (do
      (println "nothing to do, sha is already known: " sha)
      sha)
    (let [sha (->> v (coll->tree dict) mktree)]
      (println "Wrote collection to disk, sha: " sha)
      (swap! dict assoc-in [:sha->obj sha] v)
      (swap! dict assoc-in [:obj->sha v] sha)
      sha)))

(defn write-coll-with-dict
  [hdict v]
  (let [dict (atom hdict)
        result (write-coll* dict v)]
    [@dict result]))

(defn write-coll
  [v]
  (second (write-coll-with-dict {} v)))

;; ----

(declare read-coll*)

(defn read-blob [hsh]
  (git [:show (str hsh ":root")]))

(defn blob->coll
  [dict st]
  (edn/read-string {:readers {'kobold.core/rf (partial read-coll* dict)}} st))

(defn read-coll*
  "Takes the hash of a tree object and reads the collection stored"
  [dict hsh]
  (if (get-in @dict [:sha->obj hsh])
    (do
      (println "already in dict: " hsh)
      (get-in @dict [:sha->obj hsh]))
    (let [v (->> hsh read-blob (blob->coll dict))]
      (println "reading from storage: " hsh)
      (swap! dict assoc-in [:sha->obj hsh] v)
      (swap! dict assoc-in [:obj->sha v] hsh)
      v)))

(defn read-coll-with-dict
  "Returns a pair [updated-hdict coll]"
  [hdict hsh]
  (assert (= :tree (obj-type hsh)))
  (let [dict (atom hdict)
        result (read-coll* dict hsh)]
    [@dict result]))

(defn read-coll
  [hsh]
  (->> hsh (read-coll-with-dict nil) second))

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
  [dict v]
  (let [parent (read-head)
        [updated-dict tree-hash] (write-coll-with-dict dict v)]
    (-> (commit-tree tree-hash parent)
        advance-head)
    updated-dict))

(defn show-head
  []
  (-> (read-head) commit-hash->tree-hash read-coll))

(deftype GitBackend [dict-atom]
  IDurableBackend
  (-commit!
    [this new-val]
    (let [updated-dict (commit-coll @dict-atom new-val)]
      (reset! dict-atom updated-dict)
      true))
  (-remove!
    [this]
    true
    ))

(defn git-atom
  [init]
  (let [[dict val] (->> (read-head)
                        commit-hash->tree-hash
                        (read-coll-with-dict {}))
        dict-atom (atom dict)]
    (println "new dict:" dict)
    (atom* val (GitBackend. dict-atom) {})))
