(defproject ronda/routing-bidi "0.1.0"
  :description "ronda RouteDescriptor for bidi."
  :url "https://github.com/xsc/ronda-routing-bidi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/tools.reader "0.8.15"]
                 [ronda/routing "0.1.0"]
                 [clj-time "0.9.0"]
                 [bidi "1.18.7"
                  :exclusions [org.clojure/clojurescript
                               com.keminglabs/cljx
                               commons-fileupload]]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [joda-time "2.7"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
