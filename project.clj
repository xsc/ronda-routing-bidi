(defproject ronda/routing-bidi "0.1.0-SNAPSHOT"
  :description "ronda RouteDescriptor for bidi."
  :url "https://github.com/xsc/ronda/tree/master/ronda-routing-bidi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [ronda/routing "0.1.0-SNAPSHOT"]
                 [bidi "1.15.0" :exclusions [com.keminglabs/cljx]]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [joda-time "2.7"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
