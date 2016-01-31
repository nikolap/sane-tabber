(ns sane-tabber.controllers.generic
  (:require [ajax.core :refer [GET]]
            [sane-tabber.session :refer [insert! errors]]
            [sane-tabber.utils :refer [dispatch!]]))

(defn reset-notification [msg-store]
  (js/setTimeout (fn [] (reset! msg-store nil)) 5000))

(defn error-handler [{:keys [status status-text]}]
  (if (= 403 status)
    (dispatch! "/")
    (do
      (reset! errors (str "Code " status ". Unable to process request to server. " status-text))
      (reset-notification errors))))

(defn get-req [uri params]
  (GET uri
       (merge params {:error-handler   error-handler
                      :response-format :transit})))

(defn basic-get [uri k]
  (get-req uri {:handler #(insert! k %)}))