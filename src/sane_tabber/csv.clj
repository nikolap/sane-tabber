(ns sane-tabber.csv
  (:require [clojure-csv.core :as csv]))

(defn write-csv
  [map-sequence header & {:as opts}]
  (let [opts (vec (reduce concat (vec opts)))
        data (map (fn [line]
                    (vec (map (fn [item]
                                (str (get line item)))
                              header)))
                  map-sequence)]
    (apply csv/write-csv (cons (map name header) data) opts)))