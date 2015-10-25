(ns sane-tabber.core
  (:require [reagent.core :as reagent]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [ajax.core :refer [GET POST]]
            [sane-tabber.routes :refer [page]]
            [sane-tabber.views.navigation :refer [sidebar]])
  (:import goog.History))

(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          EventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

(defn mount-components []
  (reagent/render [#'sidebar] (.getElementById js/document "sidebar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-components))
