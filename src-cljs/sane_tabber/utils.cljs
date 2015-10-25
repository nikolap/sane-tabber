(ns sane-tabber.utils
  (:require [dommy.core :as dom :refer-macros [sel1]]))

(defn id-value [id]
  (when-let [elem (sel1 id)]
    (dom/value elem)))

(defn index-of [coll v]
  (let [i (count (take-while #(not= v %) coll))]
    (when (or (< i (count coll))
              (= v (last coll)))
      i)))

(def filter-first (comp first filter))

(defn remove-when [coll item]
  (remove #(= % item) coll))

(defn dispatch! [uri]
  (set! (.-href js/location) uri))