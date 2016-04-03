(ns sane-tabber.editors.teams.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [dommy.core :as dom]
            [sane-tabber.generic.data :refer [add-or-update get-by-id remove-item add-item]]
            [sane-tabber.websockets :as ws]
            [sane-tabber.utils :refer [id-value id-checked]]))

(register-handler
  :update-speakers
  (fn [db [_ speaker]]
    (add-or-update db :speakers speaker :_id)))

(register-handler
  :update-teams
  (fn [db [_ team]]
    (add-or-update db :teams team :_id)))

(register-handler
  :update-speaker
  (fn [db [_ speaker new-name]]
    (ws/send-transit-msg! (assoc speaker :name new-name) :speakers)
    db))

(register-handler
  :create-speaker
  (fn [db [_ speaker]]
    (ws/send-transit-msg! speaker :speakers)
    db))

(register-handler
  :submit-new-speaker
  (fn [db _]
    (if (clojure.string/blank? (id-value :#new-speaker-name))
      (js/alert "Please enter a name")
      (do
        (dispatch [:create-speaker {:name          (id-value :#new-speaker-name)
                                    :tournament-id (:active-tournament db)}])
        (dom/set-value! (id-value :#new-speaker-name) nil)))
    db))

(register-handler
  :create-team
  (fn [db [_ team]]
    (ws/send-transit-msg! team :teams)
    db))

(register-handler
  :submit-new-team
  (fn [db _]
    (dispatch [:create-team {:school-id     (id-value :#new-team-school-id)
                             :team-code     (id-value :#new-team-code)
                             :accessible?   (id-checked :#new-team-accessible)
                             :signed-in?    (id-checked :#new-team-signed-in)
                             :tournament-id (:active-tournament db)}])
    db))