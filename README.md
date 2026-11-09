# jolt-lang/router (reitit on Jolt)

[reitit](https://github.com/metosin/reitit) routing for
[Jolt](https://github.com/jolt-lang/jolt).

reitit-core's fast router is built on a Java class
([`reitit.Trie`](https://github.com/metosin/reitit/blob/master/modules/reitit-core/java-src/reitit/Trie.java)).
Jolt has no JVM, so `reitit.trie-jolt` reimplements that trie in Clojure and
registers it as a `reitit.Trie` host class through jolt's class-shim hooks —
then reitit-core itself loads **unmodified** from git and routes normally.

## Usage

Add the dependency and use reitit-core as usual. reitit-core and meta-merge
come along with it:

```clojure
;; deps.edn
:deps {jolt-lang/router {:git/url "https://github.com/jolt-lang/router" :git/sha "..."}}
```

```clojure
(require '[reitit.core :as r])

(def router
  (r/router [["/api/users"      :users]
             ["/api/users/:id"  :user]
             ["/files/*path"    :files]]))

(r/match-by-path router "/api/users/42")
;; => #reitit.core.Match{:data :user :path-params {:id "42"} ...}
```

There is nothing to require first. deps.edn declares `reitit.Trie` under
`:jolt/provides`, so reitit's first reference to the class loads
`reitit.trie-jolt`. reitit's `:clj` branches are the ones jolt reads by default.

The [ring-app example](https://github.com/jolt-lang/examples/tree/main/ring-app)
uses it to route a real web app.

## Tests

```bash
jolt -M:test
```
