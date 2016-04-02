(ns sane-tabber.editors.rooms.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [sane-tabber.generic.data :refer [add-or-update]]
            [sane-tabber.websockets :as ws]))

(register-handler
  :update-rooms
  (fn [db [_ room]]
    (add-or-update db :rooms room :_id)))

(register-handler
  :create-room
  (fn [db [_ room]]
    (ws/send-transit-msg! room)
    db))