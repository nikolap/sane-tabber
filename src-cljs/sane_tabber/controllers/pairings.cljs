(ns sane-tabber.controllers.pairings
  (:require [reagent.session :as session]
            [ajax.core :refer [POST]]
            [dommy.core :as dom :refer-macros [sel1 sel]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.utils :refer [id-value duplicates?]]
            [sane-tabber.session :refer [get-by-id add-or-update! app-state]]))

(defn update-round-rooms! [msg]
  (add-or-update! :round-rooms msg :_id))

(defn unused-rooms [rooms round-rooms & [id]]
  (filter #(or (and (not (contains? (set (map :room round-rooms)) (:_id %)))
                    (not (:disabled? %)))
               (= (:_id %) id)) rooms))

(defn unused-judges [judges round-rooms & [id]]
  (filter #(or (and (not (contains? (set (mapcat :judges round-rooms)) (:_id %)))
                    (:signed-in? %))
               (= (:_id %) id)) judges))

(defn unused-teams [teams round-rooms & [id]]
  (filter #(or (and (not (contains? (set (map name (mapcat (comp keys :teams) round-rooms))) (:_id %)))
                    (:signed-in? %))
               (= (:_id %) id)) teams))

(defn create-round-room [round-room]
  (ws/send-transit-msg! round-room :round-rooms))

(defn submit-new-team [judges]
  (let [new-rr-room (id-value :#new-rr-room)
        new-rr-teams (map dom/value (sel ".new-team-select"))
        new-judges (map :_id @judges)]
    (cond
      (empty? new-rr-room) (js/alert "Please select a room")
      (some empty? new-rr-teams) (js/alert "Please select teams")
      (duplicates? new-rr-teams) (js/alert "Please select unique teams")
      (empty? new-judges) (js/alert "Please select at least one judge")
      :else (do
              (create-round-room {:room          new-rr-room
                                  :judges        new-judges
                                  :teams         (apply merge
                                                        (map-indexed (fn [i t]
                                                                       {t i}) new-rr-teams))
                                  :tournament-id (session/get :tid)
                                  :round-id      (session/get :rid)})
              (reset! judges #{})))))

(defn check-round-paired [{:keys [teams round-rooms]}]
  (let [teams (unused-teams teams round-rooms)]
    (when (empty? teams)
      (POST (str "/ajax/tournaments/" (session/get :tid) "/rounds/" (session/get :rid) "/status")
            {:headers {:x-csrf-token (id-value :#__anti-forgery-token)}
             :params  {:status "paired"}}))))

(defn update-round-room-room [rr new-room]
  (ws/send-transit-msg! (assoc rr :room new-room) :round-rooms))

(defn update-round-room-teams [rr new-team position]
  (ws/send-transit-msg! (assoc rr :teams
                                  (assoc (into {} (filter #(not= position (second %)) (:teams rr))) new-team position))
                        :round-rooms)
  #_(check-round-paired @app-state))

(defn update-round-room-judges [rr new-judges]
  (ws/send-transit-msg! (assoc rr :judges new-judges) :round-rooms))

(defn add-rr-judge [rr-id new-judge]
  (let [rr (get-by-id :round-rooms rr-id :_id)]
    (update-round-room-judges rr (conj (:judges rr) new-judge))))

(defn remove-rr-judge [rr-id old-judge]
  (let [rr (get-by-id :round-rooms rr-id :_id)]
    (update-round-room-judges rr (filter #(not= % old-judge) (:judges rr)))))