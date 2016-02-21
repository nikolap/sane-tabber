(ns sane-tabber.utils
  (:require [cognitect.transit :as transit])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

(defn oid-str [item]
  (if (coll? item) (mapv str item) (str item)))

(defn stringify-reduce [m ks]
  (reduce #(update-in %1 [%2] oid-str) m ks))

(defn stringify-map [coll ks]
  (map #(stringify-reduce % ks) coll))

(defn read-transit-msg [msg]
  (-> msg
      .getBytes
      ByteArrayInputStream.
      (transit/reader :json)
      transit/read))

(defn write-transit-str [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toString out)))

(defn wrap-transit-resp [resp]
  {:status  200
   :headers {"Content-Type" "applications/transit+json; charset=utf-8"}
   :body    (write-transit-str resp)})

(defn inc* [n]
  (if n (inc n) 0))

(defn max-inc [coll]
  (if (empty? coll) 1 (inc (apply max coll))))

(def filter-first (comp first filter))