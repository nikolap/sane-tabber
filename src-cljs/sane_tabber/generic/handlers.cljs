(ns sane-tabber.generic.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ajax.core :refer [GET POST]]
            [sane-tabber.utils :refer [id-value dispatch!]]))

(defn kw-prefix [kw prefix]
  (->> kw name (str prefix) keyword))

(defn basic-set-handler
  ([handler-kw kw]
   (register-handler
     handler-kw
     (fn [db [_ v]]
       (assoc db kw v))))
  ([k]
   (basic-set-handler (kw-prefix k "set-") k)))

(defn basic-get-handler
  ([uri handler-kw kw]
   (register-handler
     handler-kw
     (fn [db _]
       (dispatch [:basic-get uri kw])
       db)))
  ([uri k]
   (basic-get-handler
     uri
     (kw-prefix k "get-")
     k)))

(register-handler
  :init-db
  (fn [_ _]
    {:x-csrf-token        (id-value :#__anti-forgery-token)}))

(register-handler
  :error-resp
  (fn [db [_ resp]]
    (cond
      (= 403 (:status resp)) (do (dispatch! "#/") db)
      (map? resp) (assoc db :errors (str "Code " (:status resp) ". Unable to process request to server. " (:status-text resp)))
      :else (assoc db :errors resp))))

(register-handler
  :form-error-resp
  (fn [db [_ resp k]]
    (if (= 403 (:status resp))
      (do (dispatch! "#/") db)
      (assoc db :errors {k resp}))))

(register-handler
  :succes-basic-get
  (fn [db [_ k resp]]
    (assoc db k resp)))

(register-handler
  :get-req
  (fn [db [_ uri params]]
    (GET uri
         (merge params {:error-handler   #(dispatch [:error-resp %1])
                        :response-format :transit}))
    db))

(register-handler
  :basic-get
  (fn [db [_ uri k]]
    (dispatch [:get-req uri {:handler #(dispatch [:succes-basic-get k %1])}])
    db))

(register-handler
  :get-users
  (fn [db [_ tid]]
    (dispatch [:basic-get (str "/ajax/tournaments/" tid "/users") :users])
    db))

(register-handler
  :get-tournament
  (fn [db [_ tid]]
    (dispatch [:basic-get (str "/ajax/tournaments/" tid "/") :tournament])
    db))

(basic-set-handler :active-page)
(basic-set-handler :active-tournament)
(basic-set-handler :tournament)
(basic-set-handler :tooltip-data)

(basic-get-handler "ajax/tournaments" :tournaments)