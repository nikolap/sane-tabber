(ns sane-tabber.generic.utils
  (:require [sane-tabber.generic.data :refer [get-by-id]]))

(defn format-team-name [{:keys [school-id team-code]} schools]
  (str (:name (get-by-id schools school-id :_id)) " " team-code))