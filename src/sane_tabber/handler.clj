(ns sane-tabber.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [sane-tabber.layout :refer [error-page]]
            [sane-tabber.routes.auth :refer [auth-routes]]
            [sane-tabber.routes.tabber :refer [tabber-routes]]
            [sane-tabber.routes.websockets :refer [websocket-routes]]
            [sane-tabber.middleware :as middleware]
            [clojure.tools.logging :as log]
            [compojure.route :as route]
            [config.core :refer [env]]
            [sane-tabber.config :refer [defaults]]
            [mount.core :as mount]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (when-let [config (:log-config env)]
    (org.apache.log4j.PropertyConfigurator/configure config))
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  ((:init defaults)))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (log/info "sane-tabber is shutting down...")
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (log/info "shutdown complete!"))

(def app-routes
  (routes
    (-> #'auth-routes
        (wrap-routes middleware/wrap-formats)
        (wrap-routes middleware/wrap-csrf))
    (-> #'tabber-routes
        (wrap-routes middleware/wrap-formats)
        (wrap-routes middleware/wrap-csrf))
    (wrap-routes #'websocket-routes middleware/wrap-csrf)
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
