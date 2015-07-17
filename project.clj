(defproject ronda/routing-bidi "0.1.3-SNAPSHOT"
  :description "ronda RouteDescriptor for bidi."
  :url "https://github.com/xsc/ronda-routing-bidi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.9.2"]
                 [ronda/routing "0.2.7"]
                 [clj-time "0.10.0"]
                 [bidi "1.20.1"
                  :exclusions [org.clojure/clojurescript
                               com.keminglabs/cljx
                               commons-fileupload]]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]
                                  [joda-time "2.8.1"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
