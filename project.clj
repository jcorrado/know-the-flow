(defproject know-the-flow "0.2.0-SNAPSHOT"
  :description "Know The Flow"
  :url "https://github.com/jcorrado/know-the-flow"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [clj-serial "2.0.3"]
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.0"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.6.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.7"]]
  :main ^:skip-aot know-the-flow.core
  :ring {:handler know-the-flow.core/-main}
  :plugins [[lein-ring "0.12.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]]}})
