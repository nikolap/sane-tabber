(ns sane-tabber.routes
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [ajax.core :refer [GET]]
            [sane-tabber.session :refer [app-state assoc-resp]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.controllers.editors.rooms :refer [update-rooms!]]
            [sane-tabber.views.home :refer [home-page]]
            [sane-tabber.views.new-tournament :refer [new-tournament-page]]
            [sane-tabber.views.dashboard :refer [dashboard-page]]
            [sane-tabber.views.editors.rooms :refer [rooms-editor-page]]))

(secretary/set-config! :prefix "#")

(def pages
  {:home           #'home-page
   :new-tournament #'new-tournament-page
   :dashboard      #'dashboard-page
   :room-editor    #'rooms-editor-page})

(defn page []
  [(pages (session/get :page))])

(secretary/defroute "/" []
                    (GET "/ajax/tournaments"
                         {:handler #(assoc-resp % :tournaments)})
                    (session/put! :page :home))
(secretary/defroute "/new" []
                    (session/put! :page :new-tournament))
(secretary/defroute "/:tid/dashboard" [tid]
                    (session/put! :tid tid)
                    (session/put! :page :dashboard))
(secretary/defroute "/:tid/editor/rooms" [tid]
                    (session/put! :tid tid)
                    (GET (str "/ajax/tournaments/" tid "/rooms")
                         {:handler #(assoc-resp % :rooms)})
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/rooms") update-rooms!)
                    (session/put! :page :room-editor))