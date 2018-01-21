(ns sane-tabber.generic.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ajax.core :refer [GET POST]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.utils :refer [id-value dispatch!]]
            [sane-tabber.generic.data :refer [add-or-update]]))

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

(defn basic-tournament-get [uri-fn kw]
  (register-handler
    (kw-prefix kw "get-")
    (fn [db [_ tid]]
      (dispatch [:basic-get (uri-fn tid) kw])
      db)))

(register-handler
  :ws-update
  (fn [db [_ item new-val k ws-kw]]
    (ws/send-transit-msg! (assoc item k new-val) ws-kw)
    db))

(register-handler
  :init-db
  (fn [_ _]
    {:x-csrf-token (id-value :#__anti-forgery-token)}))

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
  :send-transit-toggle
  (fn [db [_ item k path]]
    (ws/send-transit-msg! (update item k not) path)
    db))

(basic-set-handler :active-page)
(basic-set-handler :active-tournament)
(basic-set-handler :active-round)
(basic-set-handler :tournament)
(basic-set-handler :tooltip-data)

(basic-get-handler "ajax/tournaments" :tournaments)
(basic-tournament-get #(str "/ajax/tournaments/" % "/") :tournament)
(basic-tournament-get #(str "/ajax/tournaments/" % "/users") :users)
(basic-tournament-get #(str "/ajax/tournaments/" % "/teams") :teams)
(basic-tournament-get #(str "/ajax/tournaments/" % "/rounds") :rounds)
(basic-tournament-get #(str "/ajax/tournaments/" % "/rooms") :rooms)
(basic-tournament-get #(str "/ajax/tournaments/" % "/judges") :judges)
(basic-tournament-get #(str "/ajax/tournaments/" % "/scratches") :scratches)
(basic-tournament-get #(str "/ajax/tournaments/" % "/schools") :schools)
(basic-tournament-get #(str "/ajax/tournaments/" % "/speakers") :speakers)

(register-handler
  :get-round-rooms
  (fn [db [_ tid rid]]
    (dispatch [:basic-get (str "/ajax/tournaments/" tid "/" rid "/round-rooms") :round-rooms])
    db))

(register-handler
  :update-round-rooms
  (fn [db [_ round-room]]
    (add-or-update db :round-rooms round-room :_id)))

(register-handler
  :get-stats
  (fn [db [_ tid rid]]
    (dispatch [:basic-get (str "/ajax/tournaments/" tid "/" rid "/pairing-stats") :stats])
    db))