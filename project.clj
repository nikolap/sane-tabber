(defproject sane-tabber "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [selmer "1.0.0"]
                 [markdown-clj "0.9.85"]
                 [luminus/config "0.3"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "1.0.0"]
                 [org.webjars/bootstrap "3.3.6"]
                 [org.webjars/jquery "2.2.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.13"]
                 [org.apache.logging.log4j/log4j-core "2.5"]
                 [com.taoensso/tower "3.0.2"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring "1.4.0" :exclusions [ring/ring-jetty-adapter]]
                 [mount "0.1.8"]
                 [luminus-nrepl "0.1.2"]
                 [buddy "0.9.0"]
                 [com.novemberain/monger "3.0.0-rc2"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.13"]
                 [reagent-utils "0.1.7"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-ajax "0.5.3"]
                 [clojurewerkz/mailer "1.2.0"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [luminus-immutant "0.1.0"]
                 [prismatic/dommy "1.1.0"]
                 [org.clojure/data.csv "0.1.3"]]

  :min-lein-version "2.0.0"
  :uberjar-name "sane-tabber.jar"
  :jvm-opts ["-server"]
  :resource-paths ["resources" "target/cljsbuild"]

  :main sane-tabber.core

  :plugins [[lein-environ "1.0.1"]
            [lein-cljsbuild "1.1.1"]]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
                   {:output-to    "target/cljsbuild/public/js/app.js"
                    :output-dir   "target/cljsbuild/public/js/out"
                    :externs      ["react/externs/react.js"]
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
   :prd           [:project/prod :profiles/prod]
   :project/dev   {:dependencies   [[prone "1.0.1"]
                                    [ring/ring-mock "0.3.0"]
                                    [ring/ring-devel "1.4.0"]
                                    [pjstadig/humane-test-output "0.7.1"]
                                    [lein-figwheel "0.5.0-3"]
                                    [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                    [org.clojure/tools.namespace "0.3.0-alpha3"]]
                   :plugins        [[lein-figwheel "0.5.0-3"] [org.clojure/clojurescript "1.7.228"]]
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
                                    :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                                    :css-dirs         ["resources/public/css"]
                                    :ring-handler     sane-tabber.handler/app}

                   :source-paths   ["env/dev/clj"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options   {:init-ns user}
                   :injections     [(require 'pjstadig.humane-test-output)
                                    (pjstadig.humane-test-output/activate!)]
                   ;;when :nrepl-port is set the application starts the nREPL server on load
                   :env            {:dev        true
                                    :port       3000
                                    :nrepl-port 7000}}
   :project/test  {:env {:test       true
                         :port       3001
                         :nrepl-port 7001}}
   :project/prod  {:env        {:prod true
                                :port 80}
                   :cljsbuild
                               {:builds
                                {:app
                                 {:source-paths ["env/prod/cljs"]
                                  :compiler
                                                {:optimizations :advanced
                                                 :pretty-print  false
                                                 :closure-warnings {:externs-validation :off
                                                                    :non-standard-jsdoc :off}}}}}
                   :prep-tasks ["clean"
                                "compile" ["cljsbuild" "once"]]}
   :profiles/dev  {}
   :profiles/test {}
   :profiles/prod {}})
