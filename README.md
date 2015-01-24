# ronda-routing-bidi

__ronda-routing-bidi__ offers a [ronda-routing](https://github.com/xsc/ronda-routing) `RouteDescriptor` for [bidi](https://github.com/juxt/bidi).

[![Build Status](https://travis-ci.org/xsc/ronda-routing-bidi.svg)](https://travis-ci.org/xsc/ronda-routing-bidi)

## Usage

Don't. But if there really is nothing that can be done to stop you:

__Leiningen__ ([via Clojars](http://clojars.org/ronda/routing-bidi))

[![Clojars Project](http://clojars.org/ronda/routing-bidi/latest-version.svg)](http://clojars.org/ronda/routing-bidi)

__REPL__

Use your bidi route spec to generate a `RouteDescriptor`:

```clojure
(require '[ronda.routing.bidi :as bidi])

(def routes
  (bidi/descriptor
    ["/app" {"/articles" {""        :articles
                          ["/" :id] :article}
             "/home" :home}]))

(def app
  (-> handler
      ;; ...
      (ronda.routing/wrap-routing routes)))
```

__Direct Usage__

```clojure
(require '[ronda.routing.descriptor :as describe])

(describe/routes routes)
;; => {:home     {:path "/app/home"},
;;     :articles {:path "/app/articles"},
;;     :article  {:path ["/app/articles/" :id]}}

(describe/match routes :get "/app/articles/123")
;; => {:id :article, :route-params {:id "123"}}

(describe/generate routes :article {:id "123", :full true})
;; => {:path "/app/articles/123",
;;     :route-params {:id "123"},
;;     :query-params {:full "true"}}
```

## License

Copyright &copy; 2015 Yannick Scherer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
