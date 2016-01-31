(ns sane-tabber.session
  (:require [reagent.core :as reagent]
            [cljs.reader :refer [read-string]]
            [sane-tabber.utils :refer [remove-when filter-first]]))

(defonce app-state (reagent/atom {}))
(defonce errors (reagent/atom nil))

(defn insert! [k v]
  (swap! app-state assoc k v))

(defn update! [k f & args]
  (apply swap! app-state update-in (if (= "clojure.lang.PersistentVector" (type k))
                                     k
                                     [k]) f args))

(defn get-by-id [k id id-key]
  (filter-first #(= (id-key %) id) (get @app-state k)))

(defn add-item! [k item]
  (update! k conj item))

(defn remove-item! [k item]
  (update! k remove-when item))

(defn remove-set-item! [k item]
  (update! k disj item))

(defn update-item! [k old new]
  (update! k #(replace %2 %1) {old new}))

(defn add-or-update! [k new id-key]
  (if-let [old (get-by-id k (id-key new) id-key)]
    (update-item! k old new)
    (add-item! k new)))