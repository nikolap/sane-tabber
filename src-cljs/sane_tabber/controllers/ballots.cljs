(ns sane-tabber.controllers.ballots
  (:require [reagent.session :as session]
    [sane-tabber.utils :refer [event-value]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.session :refer [app-state]]
            [sane-tabber.controllers.generic :refer [basic-get]]
            [sane-tabber.controllers.pairings :refer [update-round-rooms!]]))

(defn on-change-round! [e]
  (let [rid (event-value e)]
    (swap! app-state assoc :active-round rid :round-rooms nil)
    (when (:round-rooms @ws/ws-chan)
      (ws/disconnect-websocket! :round-rooms))
    (when-not (empty? rid)
      (basic-get (str "/ajax/tournaments/" (session/get :tid) "/" rid "/round-rooms") :round-rooms)
      (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" (session/get :tid) "/" rid "/round-rooms") update-round-rooms! :round-rooms))))

(defn submit-ballot! [round-room ballot-data]
  (ws/send-transit-msg! (assoc round-room :ballot ballot-data) :round-rooms))