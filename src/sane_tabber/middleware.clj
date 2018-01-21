(ns sane-tabber.middleware
  (:require [sane-tabber.layout :refer [*app-context* error-page]]
            [clojure.tools.logging :as log]
            [config.core :refer [env]]
            [ring.middleware.flash :refer [wrap-flash]]
            [immutant.web.middleware :refer [wrap-session]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [ring.middleware.ssl :refer [wrap-hsts wrap-ssl-redirect wrap-forwarded-scheme]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [sane-tabber.layout :refer [*identity*]]
            [sane-tabber.config :refer [defaults]]
            [sane-tabber.auth :refer [rules]])
  (:import [javax.servlet ServletContext]))

(defn wrap-context [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specified in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t)
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title  "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (wrap-restful-format handler {:formats [:json-kw :transit-json :transit-msgpack]}))

(defn on-error [request response]
  (error-page
    {:status 403
     :title  (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler  authenticated?
                     :on-error on-error}))

(defn wrap-identity [handler]
  (fn [request]
    (binding [*identity* (get-in request [:session :identity])]
      (handler request))))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        wrap-identity
        (wrap-access-rules {:rules rules :on-error on-error})
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-ssl [handler]
  (if (env :dev)
    handler
    (-> handler
        wrap-hsts
        wrap-ssl-redirect
        wrap-forwarded-scheme)))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      ;wrap-formats
      wrap-webjars
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-context
      wrap-ssl
      wrap-internal-error
      wrap-multipart-params))
