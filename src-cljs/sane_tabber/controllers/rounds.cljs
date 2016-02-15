(ns sane-tabber.controllers.rounds
  (:require [ajax.core :refer [POST DELETE]]
            [reagent.session :as session]
            [sane-tabber.utils :refer [id-value dispatch!]]
            [sane-tabber.session :refer [add-item! remove-item!]]
            [sane-tabber.controllers.generic :refer [error-handler]]))

(defn create-round! []
  (POST (str "/ajax/tournaments/" (session/get :tid) "/rounds/new")
        {:headers         {:x-csrf-token (id-value :#__anti-forgery-token)}
         :handler         (partial add-item! :rounds)
         :error-handler   error-handler
         :response-format :transit}))

(defn delete-round! [{:keys [_id] :as round}]
  (when (js/confirm "Are you sure? Deleting a round will delete all associated rooms and ballots")
    (DELETE (str "/ajax/tournaments/" (session/get :tid) "/rounds/" _id)
            {:headers         {:x-csrf-token (id-value :#__anti-forgery-token)}
             :handler         #(remove-item! :rounds round)
             :error-handler   error-handler})))

(defn auto-pair-round [round-id]
  (POST (str "/ajax/tournaments/" (session/get :tid) "/rounds/" round-id "/autopair")
        {:headers         {:x-csrf-token (id-value :#__anti-forgery-token)}
         :handler         #(dispatch! (str "#/" (session/get :tid) "/pairings/" round-id))
         :error-handler   error-handler}))

(defn auto-pair-click [round-id status]
  (if (or (not status)
          (js/confirm "Are you sure you wish to autopair the round? This round appears to already be paired or complete."))
    (auto-pair-round round-id)))