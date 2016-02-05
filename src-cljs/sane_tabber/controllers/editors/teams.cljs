(ns sane-tabber.controllers.editors.teams
  (:require [reagent.session :as session]
            [dommy.core :as dom :refer-macros [sel1]]
            [sane-tabber.session :refer [add-or-update! app-state]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.utils :refer [id-value id-checked]]))

(defn update-teams! [msg]
  (add-or-update! :teams msg :_id))

(defn update-speakers! [msg]
  (add-or-update! :speakers msg :_id))

(defn send-transit-toggle [team k]
  (ws/send-transit-msg! (update team k #(not %)) :teams))

(defn update-accessible [team]
  (send-transit-toggle team :accessible?))

(defn update-signed-in [team]
  (send-transit-toggle team :signed-in?))

(defn update-team-code [team new-code]
  (ws/send-transit-msg! (assoc team :team-code new-code) :teams))

(defn update-school [team new-school-id]
  (ws/send-transit-msg! (assoc team :school-id new-school-id) :teams))

(defn unused-speakers
  ([speakers ids]
   (prn (map :_id speakers))
   (filter #(and (empty? (:team-id %))
                 (not (contains? (set ids) (:_id %)))) speakers))
  ([speakers]
   (filter (comp empty? :team-id) speakers)))

(defn create-team [team]
  (ws/send-transit-msg! team :teams))

(defn submit-new-team []
  (create-team {:school-id   (id-value :#new-team-school-id)
                :team-code   (id-value :#new-team-code)
                :accessible? (id-checked :#new-team-accessible)
                :signed-in?  (id-checked :#new-team-signed-in)})
  (dom/set-value! (sel1 :#new-team-code) nil))

(defn update-speaker-name [speaker new-name]
  (ws/send-transit-msg! (assoc speaker :name new-name) :speakers))

(defn update-speaker-team [speaker new-team]
  (ws/send-transit-msg! (assoc speaker :team-id new-team) :speakers))

(defn create-speaker [speaker]
  (ws/send-transit-msg! speaker :speakers))

(defn submit-new-speaker []
  (if (clojure.string/blank? (id-value :#new-speaker-name))
    (js/alert "Please enter a name")
    (create-speaker {:name          (id-value :#new-speaker-name)
                     :tournament-id (session/get :tid)})))