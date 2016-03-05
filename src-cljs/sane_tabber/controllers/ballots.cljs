(ns sane-tabber.controllers.ballots
  (:require [reagent.session :as session]
            [sane-tabber.utils :refer [event-value index-of duplicates?]]
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
      (ws/make-websocket! (str (session/get :ws-base) (.-host js/location) "/ws/" (session/get :tid) "/" rid "/round-rooms") update-round-rooms! :round-rooms))))

(defn get-team-score [active-scores team-id]
  (apply + (vals (get active-scores team-id))))

(defn get-team-points [active-scores team-id]
  (index-of (map first (sort-by #(apply + (second %)) > active-scores)) team-id))

(defn send-ballot! [round-room ballot-data]
  (ws/send-transit-msg! (assoc round-room :ballot ballot-data) :round-rooms)
  (.modal (js/$ "#ballot-modal") "hide")
  (swap! app-state assoc :active-scores {} :active-round-room nil))

(defn submit-ballot! [active-scores tournament round-room rr-teams]
  (cond
    (not= (count (mapcat vals (vals active-scores)))
          (* (:team-count tournament) (:speak-count tournament))) (js/alert "Please enter ALL speakers scores")
    (duplicates? (map (comp (partial apply +) vals) (vals active-scores))) (js/alert "Please ensure there are no ties")
    :else (send-ballot! round-room
                        {:teams    (apply merge
                                          (map #(let [team-id (:_id %)]
                                                 {team-id {:points (get-team-points active-scores team-id)
                                                           :score  (get-team-score active-scores team-id)}})
                                               rr-teams))
                         :speakers (apply merge (vals active-scores))})))

(defn clear-ballot! [round-room]
  (ws/send-transit-msg! (assoc round-room :ballot nil) :round-rooms))