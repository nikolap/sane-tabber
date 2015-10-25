(defproject sane-tabber "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.9.2"]
                 [com.taoensso/timbre "4.1.4"]
                 [com.taoensso/tower "3.0.2"]
                 [environ "1.0.1"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [org.webjars/bootstrap "3.3.5"]
                 [org.webjars/jquery "2.1.4"]
                 [buddy "0.7.2"]
                 [com.novemberain/monger "3.0.1"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [org.clojure/tools.reader "0.10.0"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.12"]
                 [reagent-utils "0.1.5"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cljs-ajax "0.5.0"]
                 [org.immutant/web "2.1.0"]
                 [clojurewerkz/mailer "1.2.0"]
                 [instar "1.0.11-SNAPSHOT"]
                 [prismatic/dommy "1.1.0"]
                 [org.clojure/data.csv "0.1.3"]]

  :min-lein-version "2.0.0"
  :uberjar-name "sane-tabber.jar"
  :jvm-opts ["-server"]

  :main sane-tabber.core

  :plugins [[lein-environ "1.0.1"]
            [lein-cljsbuild "1.1.0"]
            [lein-immutant "2.0.1"]]

  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-to "resources/public/js/app.js"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}
  
  :profiles
  {:uberjar {:omit-source true
             :env {:prod true}
              :hooks [leiningen.cljsbuild]
              :cljsbuild
              {:jar true
               :builds
               {:app
                {:source-paths ["env/prod/cljs"]
                 :compiler {:optimizations :advanced :pretty-print false}}}} 
             
             :aot :all}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :prod          [:project/prod :profiles/prod]
   :project/dev  {:dependencies [[ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.0"]
                                 [lein-figwheel "0.4.0"]]
                  :plugins [[lein-figwheel "0.4.1"]
                            [lein-kibit "0.1.2"]
                            [lein-ancient "0.6.7"]]
                   :cljsbuild
                   {:builds
                    {:app
                     {:source-paths ["env/dev/cljs"] :compiler {:source-map true}}}} 
                  
                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :nrepl-port 7002
                   :css-dirs ["resources/public/css"]
                   :ring-handler sane-tabber.handler/app}
                  
                  :repl-options {:init-ns sane-tabber.core}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3000
                        :nrepl-port 7000}}
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}
   :profiles/prod {}})
