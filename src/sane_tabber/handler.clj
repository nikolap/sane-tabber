(ns sane-tabber.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [sane-tabber.db.core :as db]
            [sane-tabber.layout :refer [error-page]]
            [sane-tabber.routes.tabber :refer [tabber-routes]]
            [sane-tabber.routes.auth :refer [auth-routes]]
            [sane-tabber.middleware :as middleware]
            [sane-tabber.routes.websockets :refer [websocket-routes]]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []

  (timbre/merge-config!
    {:level     (if (env :dev) :trace :info)
     :appenders {:rotor (rotor/rotor-appender
                          {:path "sane_tabber.log"
                           :max-size (* 512 1024)
                           :backlog 10})}})

  (if (env :dev) (parser/cache-off!))
  (db/connect!)
  (timbre/info (str
                 "\n-=[sane-tabber started successfully"
                 (when (env :dev) " using the development profile")
                 "]=-")))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "sane-tabber is shutting down...")
  (db/disconnect!)
  (timbre/info "shutdown complete!"))

(def app-routes
  (routes
    (wrap-routes #'tabber-routes middleware/wrap-csrf)
    (wrap-routes #'auth-routes middleware/wrap-csrf)
    websocket-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
