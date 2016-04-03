(ns sane-tabber.editors.judges.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [sane-tabber.generic.data :refer [add-or-update get-by-id remove-item add-item]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.utils :refer [id-value id-checked]]))

(defn get-new-scratch [{:keys [active-tournament]}]
  {:tournament-id active-tournament
   :team-id       (id-value :#new-scratch-team)
   :judge-id      (id-value :#new-scratch-judge)})

(defn get-new-judge [{:keys [active-tournament]}]
  {:tournament-id active-tournament
   :name          (id-value :#new-name)
   :rating        (id-value :#new-rating)
   :accessible?   (id-checked :#new-accessible)
   :signed-in?    (id-checked :#new-disabled)})

(register-handler
  :update-judges
  (fn [db [_ judge]]
    (add-or-update db :judges judge :_id)))

(register-handler
  :update-scratches
  (fn [db [_ scratch]]
    (if-let [old (get-by-id db :scratches (:_id scratch) :_id)]
      (remove-item db :scratches old)
      (add-item db :scratches scratch))))

(register-handler
  :send-judge
  (fn [db [_ judge]]
    (ws/send-transit-msg! judge :judges)
    db))

(register-handler
  :send-scratch
  (fn [db [_ scratch]]
    (ws/send-transit-msg! scratch :scratches)
    db))

(register-handler
  :submit-scratch
  (fn [db _]
    (let [{:keys [team-id judge-id] :as scratch} (get-new-scratch db)]
      (when (and (not-empty team-id) (not-empty judge-id))
        (dispatch [:send-scratch scratch])))
    db))

(register-handler
  :submit-judge
  (fn [db _]
    (let [{:keys [name] :as judge} (get-new-judge db)]
      (if (clojure.string/blank? name)
        (js/alert "Please enter a name for your judge!")
        (dispatch [:send-judge judge])))
    db))