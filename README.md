# jolt-lang/router (reitit on Jolt)

[reitit](https://github.com/metosin/reitit) routing for
[Jolt](https://github.com/jolt-lang/jolt).

reitit-core's fast router is built on a Java class
([`reitit.Trie`](https://github.com/metosin/reitit/blob/master/modules/reitit-core/java-src/reitit/Trie.java)).
Jolt has no JVM, so `reitit.trie-jolt` reimplements that trie in Clojure and
registers it as a `reitit.Trie` host class through jolt's class-shim hooks —
then reitit-core itself loads **unmodified** from git and routes normally.

## Usage

Load with `JOLT_FEATURES` including `clj` (so reitit reads its `:clj`
branches), require `reitit.trie-jolt` before building a router, and use
reitit-core as usual:

```clojure
(require '[reitit.trie-jolt]          ; registers the Trie mirror
         '[reitit.core :as r])

(def router
  (r/router [["/api/users"      :users]
             ["/api/users/:id"  :user]
             ["/files/*path"    :files]]))

(r/match-by-path router "/api/users/42")
;; => #reitit.core.Match{:data :user :path-params {:id "42"} ...}
```

```clojure
;; deps.edn — pull reitit-core (a monorepo) + this adapter
:deps {metosin/reitit-core {:git/url "https://github.com/metosin/reitit"
                            :git/sha "..." :deps/root "modules/reitit-core"}
       weavejester/meta-merge {:git/url "https://github.com/weavejester/meta-merge"
                               :git/sha "..."}
       jolt-lang/router {:git/url "https://github.com/jolt-lang/router" :git/sha "..."}}
```

The [ring-app example](https://github.com/jolt-lang/examples/tree/main/ring-app)
uses it to route a real web app.

## Tests

```bash
JOLT_FEATURES=clj,jolt,default jolt-deps -M:test
```
