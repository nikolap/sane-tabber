(ns sane-tabber.generic.data
  (:require [sane-tabber.utils :refer [filter-first remove-when]]))

(defn get-by-id
  ([coll id id-key]
   (filter-first #(= (get % id-key) id) coll))
  ([coll k id id-key]
   (get-by-id (get coll k) id id-key)))

(defn update-coll [coll k f & args]
  (if (vector? k)
    (apply update-in coll k f args)
    (apply update coll k f args)))

(defn add-item [coll k item]
  (update-coll coll k conj item))

(defn remove-item [coll k item]
  (update-coll coll k remove-when item))

(defn remove-set-item [coll k item]
  (update-coll coll k disj item))

(defn update-item [coll k old new]
  (update-coll coll k #(replace %2 %1) {old new}))

(defn add-or-update [coll k new id-key]
  (if-let [old (get-by-id coll k (id-key new) id-key)]
    (update-item coll k old new)
    (add-item coll k new)))