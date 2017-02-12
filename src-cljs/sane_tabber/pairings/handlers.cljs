(ns sane-tabber.pairings.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [dommy.core :as dom :refer-macros [sel]]
            [sane-tabber.generic.data :refer [get-by-id]]
            [sane-tabber.utils :refer [id-value duplicates?]]
            [sane-tabber.websockets :as ws]))

(register-handler
  :toggle-show-stats
  (fn [db _]
    (update db :show-stats? not)))

(register-handler
  :create-round-room
  (fn [db [_ round-room]]
    (ws/send-transit-msg! round-room :round-rooms)
    db))

(register-handler
  :submit-new-team-pairings
  (fn [db [_ judges]]
    (let [new-rr-room (id-value :#new-rr-room)
          new-rr-teams (map dom/value (sel ".new-team-select"))
          new-judges (map :_id @judges)]
      (cond
        (empty? new-rr-room) (js/alert "Please select a room")
        (some empty? new-rr-teams) (js/alert "Please select teams")
        (duplicates? new-rr-teams) (js/alert "Please select unique teams")
        (empty? new-judges) (js/alert "Please select at least one judge")
        :else (do
                (dispatch [:create-round-room {:room          new-rr-room
                                               :judges        new-judges
                                               :teams         (apply merge
                                                                     (map-indexed (fn [i t]
                                                                                    {t i}) new-rr-teams))
                                               :tournament-id (:active-tournament db)
                                               :round-id      (:active-round db)}])
                (reset! judges #{}))))
    db))

(register-handler
  :add-rr-judge
  (fn [db [_ rr-id new-judge]]
    (let [rr (get-by-id db :round-rooms rr-id :_id)]
      (dispatch [:ws-update rr (conj (:judges rr) new-judge) :judges :round-rooms]))
    db))

(register-handler
  :remove-rr-judge
  (fn [db [_ rr-id old-judge]]
    (let [rr (get-by-id db :round-rooms rr-id :_id)]
      (dispatch [:ws-update rr (filter #(not= % old-judge) (:judges rr)) :judges :round-rooms]))
    db))