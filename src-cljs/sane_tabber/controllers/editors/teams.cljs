(ns sane-tabber.controllers.editors.teams
  (:require [sane-tabber.session :refer [add-or-update!]]))

(defn update-teams! [msg]
  (add-or-update! :teams msg :_id))

(defn update-speakers! [msg]
  (add-or-update! :speakers msg :_id))