(ns sane-tabber.views.editors.generic
  (:require [sane-tabber.session :refer [get-by-id]]))

(defn format-team-name [{:keys [school-id team-code]}]
  (str (:name (get-by-id :schools school-id :_id)) " " team-code))