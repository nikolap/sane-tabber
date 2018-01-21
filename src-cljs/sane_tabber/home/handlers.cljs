(ns sane-tabber.home.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ajax.core :refer [DELETE]]
            [sane-tabber.utils :refer [reload]]))

(register-handler
  :delete-tournament
  (fn [db [_ id]]
    (when (js/confirm "Are you sure you wish to delete this tournament? No backsies!")
      (DELETE (str "/ajax/tournaments/" id "/delete")
              {:headers {:x-csrf-token (:x-csrf-token db)}
               :handler reload}))
    db))