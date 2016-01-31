(ns sane-tabber.controllers.editors.rooms
  (:require [sane-tabber.session :refer [app-state add-or-update!]]
            [sane-tabber.websockets :as ws]))

(defn update-rooms! [msg]
  (add-or-update! :rooms msg :_id))

(defn send-transit-toggle [room k]
  (ws/send-transit-msg! (update room k #(not %))))

(defn update-accessible [room]
  (send-transit-toggle room :accessible?))

(defn update-disabled [room]
  (send-transit-toggle room :disabled?))

(defn create-room [room]
  (ws/send-transit-msg! room))