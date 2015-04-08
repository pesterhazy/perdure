# perdure

Clojure's PERsistent data structures made DURable

## Introduction

*perdure* persists a regular clojure `atom` to disk while keeping track of its previous versions.

As storage backend, *perdure* uses `git`, so you can use regular `git` commands (such as `git log`) to inspect the history.

## Usage

````
> git init data

Initialized empty Git repository in /home/paulus/prg/perdure/data/.git/

> lein repl

````

Create a git-backed atom

````
user=> (def a (git-atom []))
W  3ea457ad900cbf1528fb25f49ee5c615a8dc94df
#'user/a
````

Update the atom. When you `swap!`, a new commit is created automatically.

````
user=> (e/swap! a conj {:name "Joe" :age 40})
W  2d97424b9230c0a15c4dc9119baf03845ac5fa7e
W  1a6407c61334a17925efb362efe522b8042e3c9f
[{:age 40, :name "Joe"}]

user=> (e/swap! a conj {:name "Bob" :age 20})
c  2d97424b9230c0a15c4dc9119baf03845ac5fa7e
W  2ba7383f307fda97cd2aa643ea19f5118bbfd07d
W  5b66859433b541c21c75fdb5bf37c2911c127ee5
[{:age 40, :name "Joe"} {:age 20, :name "Bob"}]
````

Note that we only need to write the collections that have changed and their parents.

````
user=> (e/swap! a update-in [0 :age] inc)
W  5ae443ab3272eac28214a848c946a5da55c2914c
c  2ba7383f307fda97cd2aa643ea19f5118bbfd07d
W  f17f288b0ad6f974ac481b5666e10a200f89fdf6
[{:age 41, :name "Joe"} {:age 20, :name "Bob"}]
````

We can use `git commands`:

````
> git log
commit 8955b43a0e9a5840a6ea000c5ad5db10ae974601
Author: Paulus Esterhazy <pesterhazy@gmail.com>
Date:   Wed Apr 8 18:23:39 2015 +0200

    Perdure initial commit

commit 4e6b985c13aef738d6105915c9203246e0cd9461
Author: Paulus Esterhazy <pesterhazy@gmail.com>
Date:   Wed Apr 8 18:22:14 2015 +0200

    Perdure initial commit

commit 5b82a2b48fc975613a12a0cbb3b54825945bf940
Author: Paulus Esterhazy <pesterhazy@gmail.com>
Date:   Wed Apr 8 18:21:40 2015 +0200

    Perdure initial commit

commit ba3f8db6c0ef7acc839d6cb514dda39be6a411b8
Author: Paulus Esterhazy <pesterhazy@gmail.com>
Date:   Wed Apr 8 18:19:27 2015 +0200

    Perdure commit

````
We can inspect what has changed using familiar notation:

````
> git diff 'HEAD^'

diff --git a/0/root b/0/root
index 6845dd9..b77155c 100644
--- a/0/root
+++ b/0/root
@@ -1,4 +1,4 @@
 {
-:age 40
+:age 41
 :name "Joe"
 }
diff --git a/root b/root
index 89fdfce..2a65de7 100644
--- a/root
+++ b/root
@@ -1,4 +1,4 @@
 [
-#pesterhazy.perdure.core/rf "2d97424b9230c0a15c4dc9119baf03845ac5fa7e"
+#pesterhazy.perdure.core/rf "5ae443ab3272eac28214a848c946a5da55c2914c"
 #pesterhazy.perdure.core/rf "2ba7383f307fda97cd2aa643ea19f5118bbfd07d"
 ]
````

## Contact

Contact: pesterhazy@gmail.com or @pesterhazy on Twitter

## License

Copyright © 2015 Paulus Esterhazy

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
