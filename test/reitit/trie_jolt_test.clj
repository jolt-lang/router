(ns reitit.trie-jolt-test
  (:require [reitit.trie-jolt]
            [reitit.core :as r]))

(def failures (atom 0))
(defn check [label expected actual]
  (if (= expected actual)
    (println "  ok  " label)
    (do (swap! failures inc)
        (println "  FAIL" label "— expected" (pr-str expected) "got" (pr-str actual)))))

(defn -main [& _]
  (println "reitit routing on jolt via the Trie mirror")
  (let [router (r/router [["/api/users" :users]
                          ["/api/users/:id" :user]
                          ["/api/users/:id/posts/:post-id" :post]
                          ["/files/*path" :files]])]
    (check "static route" {:name :users}
           (-> (r/match-by-path router "/api/users") :data))
    (check "no match" nil
           (r/match-by-path router "/nope"))
    (check "single path param" {:id "42"}
           (-> (r/match-by-path router "/api/users/42") :path-params))
    (check "param route data" {:name :user}
           (-> (r/match-by-path router "/api/users/42") :data))
    (check "two path params" {:id "7" :post-id "99"}
           (-> (r/match-by-path router "/api/users/7/posts/99") :path-params))
    (check "catch-all" {:path "a/b/c.txt"}
           (-> (r/match-by-path router "/files/a/b/c.txt") :path-params))
    (check "url-decoded param" {:id "a b"}
           (-> (r/match-by-path router "/api/users/a%20b") :path-params)))
  (if (pos? @failures)
    (throw (ex-info "test failures" {:n @failures}))
    (println "all checks passed")))
