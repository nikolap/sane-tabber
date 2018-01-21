(ns sane-tabber.settings.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub subscribe]]
            [sane-tabber.generic.subs :refer [basic-get-sub]]))

(defn filter-unused-users [users {:keys [editors owner-id]}]
  (filter #(not (contains? (set (conj editors owner-id)) (:_id %))) users))

(basic-get-sub :users)

(register-sub
  :unused-users
  (fn [_ _]
    (let [users      (subscribe [:users])
          tournament (subscribe [:tournament])]
      (reaction (filter-unused-users @users @tournament)))))