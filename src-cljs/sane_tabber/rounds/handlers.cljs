(ns sane-tabber.rounds.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ajax.core :refer [POST DELETE]]
            [sane-tabber.utils :refer [id-value dispatch!]]
            [sane-tabber.generic.data :refer [add-item remove-item]]))

(defn round-pairable? [{:keys [teams tournament]}]
  (zero? (mod (count (filter #(and (:signed-in? %)
                                   (not (:dropped? %))) teams)) (:team-count tournament))))

(register-handler
  :add-round
  (fn [db [_ round]]
    (add-item db :rounds round)))

(register-handler
  :remove-round
  (fn [db [_ round]]
    (remove-item db :rounds round)))

(register-handler
  :create-round
  (fn [db _]
    (POST (str "/ajax/tournaments/" (:active-tournament db) "/rounds/new")
          {:headers         {:x-csrf-token (:x-csrf-token db)}
           :handler         #(dispatch [:add-round %])
           :error-handler   #(dispatch [:error-resp %])
           :response-format :transit})
    db))

(register-handler
  :delete-round
  (fn [db [_ round]]
    (when (js/confirm "Are you sure? Deleting a round will delete all associated rooms and ballots")
      (DELETE (str "/ajax/tournaments/" (:active-tournament db) "/rounds/" (:_id round))
              {:headers       {:x-csrf-token (:x-csrf-token db)}
               :handler       #(dispatch [:remove-round round])
               :error-handler #(dispatch [:error-resp %])}))
    db))

(register-handler
  :auto-pair-post
  (fn [db [_ uri round-id]]
    (POST uri
          {:headers       {:x-csrf-token (:x-csrf-token db)}
           :handler       #(dispatch! (str "#/" (:active-tournament db) "/pairings/" round-id))
           :error-handler #(dispatch [:error-resp %])})
    db))

(register-handler
  :auto-pair-round
  (fn [db [_ round-id]]
    (if (round-pairable? db)
      (dispatch [:auto-pair-post (str "/ajax/tournaments/" (:active-tournament db) "/rounds/" round-id "/autopair") round-id])
      (js/alert "The number of teams is not divisible by the number of teams needed per round"))
    db))

(register-handler
  :auto-pair-judges
  (fn [db [_ round-id]]
    (dispatch [:auto-pair-post (str "/ajax/tournaments/" (:active-tournament db) "/rounds/" round-id "/autopair-judges-first")])
    db))

(register-handler
  :auto-pair-teams
  (fn [db [_ round-id]]
    (if (round-pairable? db)
      (dispatch [:auto-pair-post (str "/ajax/tournaments/" (:active-tournament db) "/rounds/" round-id "/autopair-teams-existing") round-id])
      (js/alert "The number of teams is not divisible by the number of teams needed per round"))
    db))

(register-handler
  :auto-pair-click
  (fn [db [_ round-id status autopair-handler]]
    (when (or (not= "paired" status)
              (js/confirm "Are you sure you wish to autopair the round? This round appears to already be paired."))
      (dispatch [autopair-handler round-id]))
    db))