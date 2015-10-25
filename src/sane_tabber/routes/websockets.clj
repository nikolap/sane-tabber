(ns sane-tabber.routes.websockets
  (:require [compojure.core :refer [GET POST defroutes context]]
            [taoensso.timbre :as timbre]
            [immutant.web.async :as async]
            [cognitect.transit :as t]
            [sane-tabber.db.core :as db]
            [sane-tabber.utils :refer [stringify-reduce]])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defonce channels (atom {}))

(defn add-or-update [coll item]
  (if coll (conj coll item) (vector item)))

(defn get-origin-uri [ch]
  (-> ch async/originating-request :uri))

(defn connect! [channel]
  (timbre/info "channel open")
  (swap! channels update (get-origin-uri channel) #(add-or-update % channel)))

(defn disconnect! [channel {:keys [code reason]}]
  (timbre/info "close code:" code "reason:" reason)
  (swap! channels update (get-origin-uri channel) #(remove #{channel} %)))

(defn websocket-callbacks [notify-fn!]
  {:on-open    connect!
   :on-close   disconnect!
   :on-message notify-fn!})

(defn notify-clients! [channel msg]
  (let [out (ByteArrayOutputStream. 4096)]
    (t/write (t/writer out :json) msg)
    (doseq [channel (get @channels (get-origin-uri channel))]
      (async/send! channel (.toString out)))
    (.close out)))

(defn rooms-notifier! [channel msg]
  (let [msg (t/read (t/reader (ByteArrayInputStream. (.getBytes msg)) :json))]
    (if (:_id msg)
      (do
        (db/update-room msg)
        (notify-clients! channel msg))
      (notify-clients! channel (stringify-reduce (db/create-room msg) [:_id :tournament-id])))))

(defn rooms-handler [request]
  (async/as-channel request (websocket-callbacks rooms-notifier!)))

(defroutes websocket-routes
           (context "/ws/:tid" [tid]
             (GET "/editor/rooms" [] rooms-handler)))