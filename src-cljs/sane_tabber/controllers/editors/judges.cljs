(ns sane-tabber.controllers.editors.judges
  (:require [reagent.session :as session]
            [sane-tabber.session :refer [app-state add-or-update! add-item! remove-item! get-by-id]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.utils :refer [id-value id-checked]]))

(defn update-judges! [msg]
  (add-or-update! :judges msg :_id))

(defn update-scratches! [msg]
  (prn msg)
  (if-let [old (get-by-id :scratches (:_id msg) :_id)]
    (remove-item! :scratches old)
    (add-item! :scratches msg)))

(defn send-transit-toggle [judge k]
  (ws/send-transit-msg! (update judge k #(not %))))

(defn update-accessible [judge]
  (send-transit-toggle judge :accessible?))

(defn update-dropped [judge]
  (send-transit-toggle judge :dropped?))

(defn create-judge [judge]
  (ws/send-transit-msg! judge))

(defn update-name [judge new-name]
  (ws/send-transit-msg! (assoc judge :name new-name)))

(defn update-rating [judge new-rating]
  (ws/send-transit-msg! (assoc judge :rating (js/parseInt new-rating))))

(defn new-judge-values []
  {:tournament-id (session/get :tid)
   :name          (id-value :#new-name)
   :rating        (id-value :#new-rating)
   :accessible?   (id-checked :#new-accessible)
   :dropped?      (id-checked :#new-disabled)})

(defn submit-new-judge []
  (let [judge (new-judge-values)]
    (if (clojure.string/blank? (:name judge))
      (js/alert "Please enter a name for your judge!")
      (create-judge judge))))

(defn send-scratch [scratch]
  (ws/send-transit-msg! scratch :scratches))

(defn submit-scratch []
  (send-scratch {:tournament-id (session/get :tid)
                 :team-id       (id-value :#new-scratch-team)
                 :judge-id      (id-value :#new-scratch-judge)}))