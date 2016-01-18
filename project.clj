(defproject ronda/routing-bidi "0.1.3-SNAPSHOT"
  :description "ronda RouteDescriptor for bidi."
  :url "https://github.com/xsc/ronda-routing-bidi"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.10.0"]
                 [ronda/routing "0.2.8"]
                 [bidi "1.25.0"
                  :exclusions [org.clojure/clojurescript
                               com.keminglabs/cljx
                               commons-fileupload]]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [joda-time "2.9.1"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
