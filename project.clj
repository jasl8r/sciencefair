(defproject sciencefair "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lib-noir "0.7.9"]
                 [compojure "1.1.6"]
                 [ring-server "0.3.1"]
                 [selmer "0.5.5"]
                 [com.taoensso/timbre "2.7.1"]
                 [com.postspectacular/rotor "0.1.0"]
                 [com.taoensso/tower "2.0.1"]
                 [markdown-clj "0.9.38"]
                 [environ "0.4.0"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [commons-validator "1.4.0"]
                 [ring-http-basic-auth "0.0.1"]
                 [org.apache.commons/commons-email "1.2"]
                 ]
  :aot :all
  :repl-options {:init-ns sciencefair.repl}
  :plugins [[lein-ring "0.8.8"]
            [lein-environ "0.4.0"]]
  :ring {:handler sciencefair.handler/app
         :init    sciencefair.handler/init
         :destroy sciencefair.handler/destroy}
  :profiles
  {:production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.2.1"]]
         :env {:selmer-dev true}}}
  :min-lein-version "2.0.0")