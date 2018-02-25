(defproject sane-tabber "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [selmer "1.11.5"]
                 [markdown-clj "1.0.2"]
                 [luminus/config "0.8"]
                 [ring-middleware-format "0.7.2"]
                 [metosin/ring-http-response "0.9.0"]
                 [bouncer "1.0.1"]
                 [org.webjars/bootstrap "3.3.6"]
                 [org.webjars/jquery "2.2.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [org.apache.logging.log4j/log4j-core "2.10.0"]
                 [com.taoensso/tower "3.0.2"]
                 [compojure "1.6.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-core "1.6.3"]
                 [mount "0.1.11"]
                 [luminus-nrepl "0.1.4"]
                 [buddy "2.0.0"]
                 [com.novemberain/monger "3.1.0" :exclusions [com.google.guava/guava]]
                 [org.clojure/clojurescript "1.9.946"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.4.474"]
                 [cljs-ajax "0.7.3"]
                 [clojurewerkz/mailer "1.3.0"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [org.webjars/typeaheadjs "0.11.1"]
                 [luminus-immutant "0.2.4"]
                 [prismatic/dommy "1.1.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 [re-frame "0.10.2"]
                 [org.clojure/tools.reader "1.1.0"]]

  :min-lein-version "2.0.0"
  :uberjar-name "sane-tabber.jar"
  :jvm-opts ["-server"]
  :resource-paths ["resources" "target/cljsbuild"]

  :main sane-tabber.core

  :plugins [[lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.7"]]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
                   {:output-to    "target/cljsbuild/public/js/app.js"
                    :output-dir   "target/cljsbuild/public/js/out"
                    :externs      ["resources/custom_externs.js"
                                   "react/externs/react.js"]
                    :pretty-print true}}}}

  :profiles
  {:uberjar       {:omit-source    true
                   :env            {:production true}
                   :prep-tasks     ["compile" ["cljsbuild" "once"]]
                   :cljsbuild
                                   {:builds
                                    {:app
                                     {:source-paths ["env/prod/cljs"]
                                      :compiler
                                                    {:optimizations :advanced
                                                     :pretty-print  false
                                                     :closure-warnings
                                                                    {:externs-validation :off :non-standard-jsdoc :off}}}}}

                   :aot            :all
                   :source-paths   ["env/prod/clj"]
                   :resource-paths ["env/prod/resources"]}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :prod          [:project/prod :profiles/prod]
   :project/dev   {:dependencies   [[ring/ring-mock "0.3.2"]
                                    [ring/ring-devel "1.6.3"]
                                    [com.cemerick/piggieback "0.2.2"]
                                    [prone "1.2.0"]
                                    [figwheel-sidecar "0.5.14"]]
                   :plugins        [[lein-figwheel "0.5.14"]
                                    [lein-ancient "0.6.15"]]
                   :cljsbuild
                                   {:builds
                                    {:app
                                     {:source-paths ["env/dev/cljs"]
                                      :compiler
                                                    {:main          "sane-tabber.app"
                                                     :asset-path    "/js/out"
                                                     :optimizations :none
                                                     :source-map    true}}}}

                   :figwheel
                                   {:http-server-root "public"
                                    :server-port      3449
                                    :nrepl-port       7002
                                    :css-dirs         ["resources/public/css"]
                                    :ring-handler     sane-tabber.handler/app}

                   :source-paths   ["env/dev/clj"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options   {:init-ns user}
                   ;;when :nrepl-port is set the application starts the nREPL server on load
                   :env            {:dev        true
                                    :port       3000
                                    :nrepl-port 7000}}
   :project/test  {:env {:test       true
                         :port       3001
                         :nrepl-port 7001}}
   :project/prod  {:env        {:prod true}
                   :source-paths   ["env/prod/clj"]
                   :resource-paths ["env/prod/resources"]
                   :cljsbuild
                               {:builds
                                {:app
                                 {:source-paths ["env/prod/cljs"]
                                  :compiler
                                                {:optimizations    :advanced
                                                 :pretty-print     false
                                                 :closure-warnings {:externs-validation :off
                                                                    :non-standard-jsdoc :off}}}}}}
   :profiles/dev  {}
   :profiles/test {}
   :profiles/prod {}})
