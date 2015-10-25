(ns sane-tabber.utils)

(defn oid-str [item]
  (if (coll? item) (mapv str item) (str item)))

(defn stringify-reduce [m ks]
  (reduce #(update-in %1 [%2] oid-str) m ks))

(defn stringify-map [coll ks]
  (map #(stringify-reduce % ks) coll))