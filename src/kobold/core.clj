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

(defn ser-vec
  "Returns a pair [blob tree-str]"
  [v]
  (let [[blob tree-str] (reduce
                         (fn [[out-str out-tree tree-count] e]
                           (if (vector? e)
                             (let [hsh (write-vec e)]
                               [(str out-str (str "#kobold.core/rf \"" hsh "\"\n"))
                                (str out-tree "040000 tree " hsh "\t" tree-count "\n")
                                (inc tree-count)])
                             [(str out-str (prn-str e)) out-tree tree-count]))
                         ["" "" 0]
                         v)]
    [(str "[\n" blob "]\n") tree-str]))

(defn vec->tree
  [v]
  "Converts a vector into a string representation of a git tree object"
  (let [[blob tree-str] (ser-vec v)
        hsh (hash-object blob)]
    (str "100644 blob " hsh "\troot\n" tree-str)))

(defn write-vec
  "Writes a vector to the db. Returns hash of the tree object"
  [v]
  (-> v vec->tree mktree))

;; (defn read-blob-line [st]
;;   (if-let [m (re-find #"#kobold.core/rf \"(.*)\"" st)]
;;     (let [hsh (get m 1)]
;;       (read-vec hsh))
;;     (read-string st)))

(defn blob->vec
  [st]
  (edn/read-string {:readers {'kobold.core/rf read-vec}} st))

(defn read-vec
  "Takes the hash of a tree object and reads the vector stored"
  [hsh]
  (blob->vec (git [:show (str hsh ":root")])))

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
