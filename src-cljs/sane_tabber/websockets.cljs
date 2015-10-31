(ns sane-tabber.websockets
  (:require [cognitect.transit :as t]))

(defonce ws-chan (atom {}))
(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(defn receive-transit-msg!
  [update-fn]
  (fn [msg]
    (update-fn
      (->> msg .-data (t/read json-reader)))))

(defn send-transit-msg!
  [msg & [path]]
  (if-let [ch (if path (get @ws-chan path) (:default @ws-chan))]
    (.send ch (t/write json-writer msg))
    (throw (js/Error. "Websocket is not available!"))))

(defn make-websocket! [url receive-handler & [path]]
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
      (swap! ws-chan assoc (if path path :default) chan))
    (throw (js/Error. "Websocket connection failed!"))))

(defn disconnect-websocket! [path]
  (.close (get @ws-chan path))
  (swap! ws-chan dissoc path))

(defn reset-channels! []
  (doseq [ws (vals @ws-chan)]
    (.close ws))
  (reset! ws-chan {}))