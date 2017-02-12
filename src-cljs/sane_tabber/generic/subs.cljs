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

(defn unused-rooms [rooms round-rooms & [id]]
  (filter #(or (and (not (contains? (set (map :room round-rooms)) (:_id %)))
                    (not (:disabled? %)))
               (= (:_id %) id)) rooms))

(defn unused-judges [judges round-rooms & [id]]
  (filter #(or (and (not (contains? (set (mapcat :judges round-rooms)) (:_id %)))
                    (:signed-in? %))
               (= (:_id %) id)) judges))

(defn unused-teams [teams round-rooms & [id]]
  (filter #(or (and (not (contains? (set (map name (mapcat (comp keys :teams) round-rooms))) (:_id %)))
                    (:signed-in? %))
               (= (:_id %) id)) teams))

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
(basic-get-sub :round-rooms)
(basic-get-sub :stats)
(basic-get-sub :show-stats?)

(register-sub
  :unused-speakers
  (fn [_ [_ ids]]
    (let [speakers (subscribe [:speakers])]
      (reaction
        (if ids (unused-speakers @speakers ids)
                (unused-speakers @speakers))))))

(register-sub
  :unused-rooms
  (fn [_ [_ id]]
    (let [rooms (subscribe [:rooms])
          round-rooms (subscribe [:round-rooms])]
      (reaction (unused-rooms @rooms @round-rooms id)))))

(register-sub
  :unused-judges
  (fn [_ [_ id]]
    (let [judges (subscribe [:judges])
          round-rooms (subscribe [:round-rooms])]
      (reaction (unused-judges @judges @round-rooms id)))))

(register-sub
  :unused-teams
  (fn [_ [_ id]]
    (let [teams (subscribe [:teams])
          round-rooms (subscribe [:round-rooms])]
      (reaction (unused-teams @teams @round-rooms id)))))
