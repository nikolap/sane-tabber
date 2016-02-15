(ns sane-tabber.routes.websockets
  (:require [compojure.core :refer [GET POST defroutes context]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as t]
            [sane-tabber.db.core :as db]
            [sane-tabber.utils :refer [stringify-reduce read-transit-msg]])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defonce channels (atom {}))

(defn add-or-update [coll item]
  (if coll (conj coll item) (vector item)))

(defn get-origin-uri [ch]
  (-> ch async/originating-request :uri))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels update (get-origin-uri channel) #(add-or-update % channel)))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
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

(defn basic-notifier [channel msg update-fn create-fn stringify-keys]
  (let [msg (read-transit-msg msg)]
    (if (:_id msg)
      (do
        (update-fn msg)
        (notify-clients! channel msg))
      (notify-clients! channel (stringify-reduce (create-fn msg) stringify-keys)))))

(defn rooms-notifier! [channel msg]
  (basic-notifier channel msg db/update-room db/create-room [:_id :tournament-id]))

(defn rooms-handler [request]
  (async/as-channel request (websocket-callbacks rooms-notifier!)))

(defn judges-notifier! [channel msg]
  (basic-notifier channel msg db/update-judge db/create-judge [:_id :tournament-id]))

(defn judges-handler [request]
  (async/as-channel request (websocket-callbacks judges-notifier!)))

(defn scratches-notifier! [channel msg]
  (basic-notifier channel msg db/delete-scratch db/create-scratch [:_id :tournament-id :team-id :judge-id]))

(defn scratches-handler [request]
  (async/as-channel request (websocket-callbacks scratches-notifier!)))

(defn teams-notifier! [channel msg]
  (basic-notifier channel msg db/update-team db/create-team [:_id :tournament-id :school-id]))

(defn teams-handler [request]
  (async/as-channel request (websocket-callbacks teams-notifier!)))

(defn speakers-notifier! [channel msg]
  (basic-notifier channel msg db/update-speaker db/create-speaker [:_id :tournament-id :team-id]))

(defn speakers-handler [request]
  (async/as-channel request (websocket-callbacks speakers-notifier!)))

(defn round-rooms-notifier! [channel msg]
  (basic-notifier channel msg db/update-round-room db/create-round-room [:_id :tournament-id :round-id :team-id :room :judges]))

(defn round-rooms-handler [request]
  (async/as-channel request (websocket-callbacks round-rooms-notifier!)))

(defroutes websocket-routes
           (context "/ws/:tid" [tid]
             (GET "/round-rooms" [] round-rooms-handler)
             (GET "/editor/rooms" [] rooms-handler)
             (GET "/editor/judges" [] judges-handler)
             (GET "/editor/scratches" [] scratches-handler)
             (GET "/editor/teams" [] teams-handler)
             (GET "/editor/speakers" [] speakers-handler)
             (GET "/:rid/round-rooms" [rid] round-rooms-handler)))