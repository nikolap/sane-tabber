(ns sane-tabber.ballots.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.generic.handlers :refer [basic-set-handler]]
            [sane-tabber.utils :refer [index-of duplicates?]]))

(defn get-team-score [active-scores team-id]
  (apply + (vals (get active-scores team-id))))

(defn get-team-points [active-scores team-id]
  (index-of (map first (sort-by second (map #(into [] [(first %) (apply + (vals (second (first (second %)))))]) (group-by first active-scores)))) team-id))

(basic-set-handler :active-round-room)

(register-handler
  :clear-ballot
  (fn [db [_ round-room]]
    (ws/send-transit-msg! (assoc round-room :ballot nil) :round-rooms)
    db))

(register-handler
  :on-change-round
  (fn [db [_ rid]]
    (when (:round-rooms @ws/ws-chan)
      (ws/disconnect-websocket! :round-rooms))
    (when-not (empty? rid)
      (ws/make-websocket! (str "/ws/" (:active-tournament db) "/" rid "/round-rooms") #(dispatch [:update-round-rooms %]) :round-rooms)
      (dispatch [:get-round-rooms (:active-tournament db) rid]))
    (assoc db :active-round rid :round-rooms nil)))

(register-handler
  :close-ballot-modal
  (fn [db _]
    (assoc db :active-scores {} :active-round-room nil)))

(register-handler
  :send-ballot
  (fn [db [_ round-room ballot-data]]
    (ws/send-transit-msg! (assoc round-room :ballot ballot-data) :round-rooms)
    (.modal (js/$ "#ballot-modal") "hide")
    (assoc db :active-scores {} :active-round-room nil)))

(register-handler
  :submit-ballot
  (fn [db [_ active-scores tournament round-room rr-teams]]
    (cond
      (not= (count (mapcat vals (vals active-scores)))
            (* (:team-count tournament) (:speak-count tournament))) (js/alert "Please enter ALL speakers scores")
      (duplicates? (map (comp (partial apply +) vals) (vals active-scores))) (js/alert "Please ensure there are no ties")
      :else (dispatch [:send-ballot round-room
                       {:teams    (apply merge
                                         (map #(let [team-id (:_id %)]
                                                {team-id {:points (get-team-points active-scores team-id)
                                                          :score  (get-team-score active-scores team-id)}})
                                              rr-teams))
                        :speakers (apply merge (vals active-scores))}]))
    db))

(register-handler
  :update-active-score
  (fn [db [_ team-id id v]]
    (assoc-in db [:active-scores team-id id] (when-not (empty? v) (js/parseInt v)))))