(ns sane-tabber.generic.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub subscribe]]))

(defn basic-get-sub
  ([name k]
   (register-sub
     name
     (fn [db _]
       (reaction (k @db)))))
  ([k]
   (basic-get-sub k k)))

(defn unused-speakers
  ([speakers ids]
   (filter #(and (empty? (:team-id %))
                 (not (contains? (set ids) (:_id %)))) speakers))
  ([speakers]
   (filter (comp empty? :team-id) speakers)))

(basic-get-sub :active-page)
(basic-get-sub :active-tournament)

(basic-get-sub :x-csrf-token)

(basic-get-sub :errors)
(basic-get-sub :successes)

(basic-get-sub :tournaments)
(basic-get-sub :tournament)
(basic-get-sub :tooltip-data)
(basic-get-sub :rounds)
(basic-get-sub :rooms)
(basic-get-sub :teams)
(basic-get-sub :judges)
(basic-get-sub :schools)
(basic-get-sub :scratches)
(basic-get-sub :speakers)

(register-sub
  :unused-speakers
  (fn [_ [_ ids]]
    (let [speakers (subscribe [:speakers])]
      (reaction
        (if ids (unused-speakers @speakers ids)
                (unused-speakers @speakers))))))

