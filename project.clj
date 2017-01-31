(defproject sciencefair "0.1.0-SNAPSHOT"
            :description "Groton Dunstable Science Fair site.  Allows for sharing of information and student registration."
            :url "http://www.gdesciencefair.org"
            :dependencies [[org.clojure/clojure "1.5.1"]
                           [lib-noir "0.7.9"]
                           [compojure "1.1.6"]
                           [ring-server "0.3.1"]
                           [selmer "0.5.5"]
                           [com.taoensso/timbre "2.7.1"]
                           [com.postspectacular/rotor "0.1.0"]
                           [com.taoensso/tower "2.0.1"]
                           [markdown-clj "0.9.38"]
                           [environ "1.1.0"]
                           [org.clojure/java.jdbc "0.3.2"]
                           [mysql/mysql-connector-java "5.1.6"]
                           [commons-validator "1.4.0"]
                           [ring-http-basic-auth "0.0.1"]
                           [org.apache.commons/commons-email "1.4"]
                           [clj-http "1.0.1"]
                           ]
            :aot :all
            :repl-options {:init-ns sciencefair.repl}
            :plugins [[lein-ring "0.10.0"]
                      [lein-environ "1.1.0"]
                      [lein-cljfmt "0.1.4"]
                      ]
            :ring {:handler sciencefair.handler/app
                   :init    sciencefair.handler/init
                   :destroy sciencefair.handler/destroy}
            :profiles {:production {:ring {:open-browser? false
                                           :stacktraces?  false
                                           :auto-reload?  false}}
                       :dev-common {:dependencies [[ring-mock "0.1.5"]
                                                   [ring/ring-devel "1.2.1"]]}
                       :dev-overrides {}
                       :dev [:dev-common :dev-overrides]}
            :min-lein-version "2.0.0")