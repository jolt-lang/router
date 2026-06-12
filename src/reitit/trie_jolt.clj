(ns reitit.trie-jolt
  "A Jolt mirror of reitit's reitit.Trie Java class
  (https://github.com/metosin/reitit/blob/master/modules/reitit-core/java-src/reitit/Trie.java).

  reitit-core's :clj path builds its router from a `reitit.Trie` matcher tree
  via static factory methods (Trie/dataMatcher, Trie/staticMatcher, ...) and
  Trie/lookup. Jolt has no JVM, so this namespace reimplements those matchers
  in Clojure and registers them as a `reitit.Trie` host class through jolt's
  __register-class-* hooks. Requiring this namespace (with JOLT_FEATURES
  including clj, so reitit reads its :clj branches) is all that's needed for
  reitit routing to work on jolt.

  A matcher is a map {:match (fn [i max path] -> match|nil) :depth n :length n};
  a match is {:params <map> :data <any>}. reitit reads .params/.data off the
  match (jolt field access) and calls Trie/lookup."
  (:require [clojure.string :as str]))

;; --- decoding (mirrors Trie.decode) ----------------------------------------
(defn- decode [s percent? plus?]
  ;; URLDecoder is a jolt host shim (ring-codec enablement)
  (if percent?
    (URLDecoder/decode (if plus? (str/replace s "+" "%2B") s) "UTF-8")
    s))

;; --- matchers (mirror the Trie.* inner classes) ----------------------------
(defn data-matcher [params data]
  (let [m {:params params :data data}]
    {:match (fn [i max _] (when (= i max) m))
     :depth 1 :length 0}))

(defn static-matcher [path child]
  (let [size (count path)]
    {:match (fn [i max p]
              (when-not (< max (+ i size))
                (loop [j 0]
                  (if (= j size)
                    ((:match child) (+ i size) max p)
                    (when (= (nth p (+ i j)) (nth path j))
                      (recur (inc j)))))))
     :depth (inc (:depth child)) :length size}))

(defn- assoc-param [match k v]
  (update match :params assoc k v))

(defn wild-matcher [key end child]
  {:match (fn [i max path]
            (when (and (< i max) (not= (nth path i) end))
              (loop [percent? false j i]
                (if (= j max)
                  (when-let [m ((:match child) max max path)]
                    (assoc-param m key (decode (subs path i max) percent? false)))
                  (let [c (nth path j)]
                    (cond
                      (= c end) (when-let [m ((:match child) j max path)]
                                  (assoc-param m key (decode (subs path i j) percent? false)))
                      (= c \%) (recur true (inc j))
                      (= c \+) (recur percent? (inc j))
                      :else    (recur percent? (inc j))))))))
   :depth (inc (:depth child)) :length 0})

(defn catch-all-matcher [key params data]
  {:match (fn [i max path]
            (when (<= i max)
              (assoc-param {:params params :data data} key
                           (decode (subs path i max) true true))))
   :depth 1 :length 0})

(defn linear-matcher [matchers ordered?]
  (let [childs (vec (if ordered?
                      matchers
                      (reverse (sort-by (juxt :depth :length) matchers))))
        size (count childs)]
    {:match (fn [i max path]
              (loop [j 0]
                (when (< j size)
                  (or ((:match (nth childs j)) i max path)
                      (recur (inc j))))))
     :depth (inc (apply max 0 (map :depth childs))) :length 0}))

(defn lookup [matcher path]
  ((:match matcher) 0 (count path) path))

;; --- registration ----------------------------------------------------------
;; reitit calls (Trie/staticMatcher ...) etc. and (Trie/lookup matcher path),
;; then reads (.params match) / (.data match) — plain keyword field access on
;; the match map, which jolt's (. obj field) handles.
(defn install! []
  ;; Character. is identity here: reitit wraps the wild end char as
  ;; (Character. end); we want the raw char.
  (__register-class-ctor! "Character" (fn [c] c))
  (__register-class-statics! "Trie"
    {"dataMatcher"     data-matcher
     "staticMatcher"   static-matcher
     "wildMatcher"     wild-matcher
     "catchAllMatcher" catch-all-matcher
     "linearMatcher"   linear-matcher
     "lookup"          lookup})
  (__register-class-statics! "reitit.Trie"
    {"dataMatcher"     data-matcher
     "staticMatcher"   static-matcher
     "wildMatcher"     wild-matcher
     "catchAllMatcher" catch-all-matcher
     "linearMatcher"   linear-matcher
     "lookup"          lookup}))

(install!)
