(ns sane-tabber.core
  (:require [reagent.core :as reagent]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [ajax.core :refer [GET POST]]
            [sane-tabber.routes :refer [page]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.navigation.views :refer [sidebar]]
            [re-frame.core :refer [dispatch-sync]]

            [sane-tabber.generic.handlers]
            [sane-tabber.generic.subs]
            [sane-tabber.home.handlers]
            [sane-tabber.new-tournament.handlers]
            [sane-tabber.rounds.handlers]
            [sane-tabber.pairings.handlers]
            [sane-tabber.ballots.handlers]
            [sane-tabber.ballots.subs]
            [sane-tabber.editors.rooms.handlers]
            [sane-tabber.editors.teams.handlers]
            [sane-tabber.editors.judges.handlers]
            [sane-tabber.settings.handlers]
            [sane-tabber.settings.subs])
  (:import goog.History))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (dispatch-sync [:init-db])
        (ws/reset-channels!)
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn mount-components []
  (reagent/render [#'sidebar] (.getElementById js/document "sidebar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-components))