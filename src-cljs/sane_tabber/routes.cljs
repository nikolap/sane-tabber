(ns sane-tabber.routes
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [ajax.core :refer [GET]]
            [sane-tabber.session :refer [app-state]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.controllers.generic :refer [basic-get]]
            [sane-tabber.controllers.pairings :refer [update-round-rooms!]]
            [sane-tabber.controllers.editors.rooms :refer [update-rooms!]]
            [sane-tabber.controllers.editors.judges :refer [update-judges! update-scratches!]]
            [sane-tabber.controllers.editors.teams :refer [update-teams! update-speakers!]]
            [sane-tabber.views.home :refer [home-page]]
            [sane-tabber.views.new-tournament :refer [new-tournament-page]]
            [sane-tabber.views.dashboard :refer [dashboard-page]]
            [sane-tabber.views.registration :refer [registration-page]]
            [sane-tabber.views.rounds :refer [rounds-page]]
            [sane-tabber.views.pairings :refer [pairings-page]]
            [sane-tabber.views.ballots :refer [ballots-page]]
            [sane-tabber.views.editors.rooms :refer [rooms-editor-page]]
            [sane-tabber.views.editors.judges :refer [judges-editor-page]]
            [sane-tabber.views.editors.teams :refer [teams-editor-page]]))

(secretary/set-config! :prefix "#")

(def pages
  {:home           #'home-page
   :new-tournament #'new-tournament-page
   :dashboard      #'dashboard-page
   :registration   #'registration-page
   :rounds         #'rounds-page
   :pairings       #'pairings-page
   :ballots        #'ballots-page
   :room-editor    #'rooms-editor-page
   :judge-editor   #'judges-editor-page
   :team-editor    #'teams-editor-page})

(defn page []
  [(pages (session/get :page))])

(secretary/defroute "/" []
                    (basic-get "ajax/tournaments" :tournaments)
                    (session/put! :page :home))
(secretary/defroute "/new" []
                    (session/put! :page :new-tournament))
(secretary/defroute "/:tid/dashboard" [tid]
                    (session/put! :tid tid)
                    (session/put! :page :dashboard))
(secretary/defroute "/:tid/registration" [tid]
                    (session/put! :tid tid)
                    (basic-get (str "/ajax/tournaments/" tid "/") :tournament)
                    (basic-get (str "/ajax/tournaments/" tid "/teams") :teams)
                    (basic-get (str "/ajax/tournaments/" tid "/schools") :schools)
                    (basic-get (str "/ajax/tournaments/" tid "/judges") :judges)
                    (basic-get (str "/ajax/tournaments/" tid "/speakers") :speakers)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/teams") update-teams! :teams)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/speakers") update-speakers! :speakers)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/judges") update-judges! :judges)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/scratches") update-scratches! :scratches)
                    (session/put! :page :registration))
(secretary/defroute "/:tid/pairings" [tid]
                    (session/put! :tid tid)
                    (basic-get (str "/ajax/tournaments/" tid "/rounds") :rounds)
                    (session/put! :page :rounds))
(secretary/defroute "/:tid/pairings/:rid" [tid rid]
                    (session/put! :tid tid)
                    (session/put! :rid rid)
                    (basic-get (str "/ajax/tournaments/" tid "/") :tournament)
                    (basic-get (str "/ajax/tournaments/" tid "/teams") :teams)
                    (basic-get (str "/ajax/tournaments/" tid "/schools") :schools)
                    (basic-get (str "/ajax/tournaments/" tid "/judges") :judges)
                    (basic-get (str "/ajax/tournaments/" tid "/rooms") :rooms)
                    (basic-get (str "/ajax/tournaments/" tid "/" rid "/round-rooms") :round-rooms)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/teams") update-teams! :teams)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/judges") update-judges! :judges)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/scratches") update-scratches! :scratches)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/rooms") update-rooms! :schoools)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/round-rooms") update-round-rooms! :round-rooms)
                    (session/put! :page :pairings))
(secretary/defroute "/:tid/ballots" [tid]
                    (session/put! :tid tid)
                    (basic-get (str "/ajax/tournaments/" tid "/") :tournament)
                    (basic-get (str "/ajax/tournaments/" tid "/teams") :teams)
                    (basic-get (str "/ajax/tournaments/" tid "/schools") :schools)
                    (basic-get (str "/ajax/tournaments/" tid "/judges") :judges)
                    (basic-get (str "/ajax/tournaments/" tid "/rooms") :rooms)
                    (basic-get (str "/ajax/tournaments/" tid "/round-rooms") :round-rooms)
                    (session/put! :page :ballots))
(secretary/defroute "/:tid/editor/rooms" [tid]
                    (session/put! :tid tid)
                    (basic-get (str "/ajax/tournaments/" tid "/rooms") :rooms)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/rooms") update-rooms!)
                    (session/put! :page :room-editor))
(secretary/defroute "/:tid/editor/judges" [tid]
                    (session/put! :tid tid)
                    (basic-get (str "/ajax/tournaments/" tid "/judges") :judges)
                    (basic-get (str "/ajax/tournaments/" tid "/teams") :teams)
                    (basic-get (str "/ajax/tournaments/" tid "/scratches") :scratches)
                    (basic-get (str "/ajax/tournaments/" tid "/schools") :schools)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/judges") update-judges! :judges)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/scratches") update-scratches! :scratches)
                    (session/put! :page :judge-editor))
(secretary/defroute "/:tid/editor/teams" [tid]
                    (session/put! :tid tid)
                    (basic-get (str "/ajax/tournaments/" tid "/") :tournament)
                    (basic-get (str "/ajax/tournaments/" tid "/teams") :teams)
                    (basic-get (str "/ajax/tournaments/" tid "/schools") :schools)
                    (basic-get (str "/ajax/tournaments/" tid "/speakers") :speakers)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/teams") update-teams! :teams)
                    (ws/make-websocket! (str "ws://" (.-host js/location) "/ws/" tid "/editor/speakers") update-speakers! :speakers)
                    (session/put! :page :team-editor))