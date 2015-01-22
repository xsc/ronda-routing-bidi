(ns ronda.routing.bidi-test
  (:require [midje.sweet :refer :all]
            [ronda.routing
             [bidi :as bidi]
             [descriptor :as describe]]))

(def digit #"\d+")

(tabular
  (fact "about bidi route listing."
        (let [d (bidi/descriptor ?routes)]
          (describe/routes d) => ?list))
  ?routes, ?list
  ["/" :endpoint]
  {:endpoint "/"}

  ["/" {"a" :a, "b" :b}]
  {:a "/a", :b "/b"}

  [["/" :id] :endpoint]
  {:endpoint ["/" :id]}

  [["/" [digit :id]] :endpoint]
  {:endpoint ["/" [digit :id]]}

  ["/" {["a/" :id] :a
        ["b/" :id] {"" :b
                    "/c" :c}}]
  {:a ["/a/" :id]
   :b ["/b/" :id]
   :c ["/b/" :id "/c"]}

  ["/" {"article" {:get :article, :put :save-article}}]
  {:article "/article"
   :save-article "/article"})


(let [d (bidi/descriptor
          ["/" {["a/" :id] :a
                "b/" {[:id] :b
                      [:id "/" :action] :c}}])]
  (tabular
    (fact "about bidi route matching."
        (let [r (describe/match d :get ?uri)]
          (:id r) => ?id
          (:route-params r) => ?route-params))
    ?uri              ?id      ?route-params
    "/a/id"           :a       {:id "id"}
    "/b/id"           :b       {:id "id"}
    "/b/id/go"        :c       {:id "id" :action "go"}
    "/unknown"        nil      nil)
  (tabular
    (fact "about bidi route generation."
          (let [r (describe/generate d ?id ?values)]
            (:path r) => ?path
            (:route-params r) => ?route-params
            (:query-params r) => ?query-params))
    ?id ?values                  ?path       ?route-params            ?query-params
    :a  {:id "id"}               "/a/id"     {:id "id"}               {}
    :a  {:id "id", :c 0}         "/a/id"     {:id "id"}               {:c "0"}
    :c  {:id "id", :action "go"} "/b/id/go"  {:id "id", :action "go"} {})
  (fact "about exceptions."
        (describe/generate d :unknown {})
        => (throws Exception #"unknown route ID")

        (describe/generate
          (bidi/descriptor [["/" [digit :id]] :x])
          :x {:id "abc"})
        => (throws Exception #"not compatible")))
