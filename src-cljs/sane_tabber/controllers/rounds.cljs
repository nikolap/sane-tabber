(ns sane-tabber.controllers.rounds
  (:require [ajax.core :refer [POST DELETE]]
            [reagent.session :as session]
            [sane-tabber.utils :refer [id-value dispatch!]]
            [sane-tabber.session :refer [app-state add-item! remove-item!]]
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
            {:headers       {:x-csrf-token (id-value :#__anti-forgery-token)}
             :handler       #(remove-item! :rounds round)
             :error-handler error-handler})))

(defn auto-pair-post [uri round-id]
  (POST uri
        {:headers       {:x-csrf-token (id-value :#__anti-forgery-token)}
         :handler       #(dispatch! (str "#/" (session/get :tid) "/pairings/" round-id))
         :error-handler error-handler}))

(defn round-pairable? []
  (zero?
    (mod (count (filter :signed-in? (:teams @app-state)))
         (get-in @app-state [:tournament :team-count]))))

(defn auto-pair-round [round-id]
  (if (round-pairable?)
    (auto-pair-post (str "/ajax/tournaments/" (session/get :tid) "/rounds/" round-id "/autopair") round-id)
    (js/alert "The number of teams is not divisible by the number of teams needed per round")))

(defn auto-pair-judges [round-id]
  (auto-pair-post (str "/ajax/tournaments/" (session/get :tid) "/rounds/" round-id "/autopair-judges-first") round-id))

(defn auto-pair-teams [round-id]
  (if (round-pairable?)
    (auto-pair-post (str "/ajax/tournaments/" (session/get :tid) "/rounds/" round-id "/autopair-teams-existing") round-id)
    (js/alert "The number of teams is not divisible by the number of teams needed per round")))

(defn auto-pair-click [round-id status autopair-fn]
  (when (or (not= "paired" status)
            (js/confirm "Are you sure you wish to autopair the round? This round appears to already be paired."))
    (autopair-fn round-id)))