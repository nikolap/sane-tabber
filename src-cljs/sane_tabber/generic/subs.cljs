(ns sane-tabber.generic.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(defn basic-get-sub
  ([name k]
   (register-sub
     name
     (fn [db _]
       (reaction (k @db)))))
  ([k]
   (basic-get-sub k k)))

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