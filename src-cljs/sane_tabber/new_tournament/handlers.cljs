(ns sane-tabber.new-tournament.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ajax.core :refer [POST]]
            [dommy.core :refer-macros [sel1]]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [sane-tabber.utils :refer [id-value dispatch!]]))

(defn validate-form []
  (let [data {:name         (id-value :#name)
              :rooms-file   (id-value :#rooms-file)
              :schools-file (id-value :#schools-file)
              :judges-file  (id-value :#judges-file)
              :teams-file   (id-value :#teams-file)}]
    (b/validate data
                :name [[v/required :message "Please enter a name for your tournament"]]
                :rooms-file [[v/required :message "Please attach a CSV of rooms"]
                             [v/matches #".+(\.csv)$" :message "You must attach a CSV file, silly"]]
                :schools-file [[v/required :message "Please attach a CSV of schools"]
                               [v/matches #".+(\.csv)$" :message "You must attach a CSV file, silly"]]
                :judges-file [[v/required :message "Please attach a CSV of judges"]
                              [v/matches #".+(\.csv)$" :message "You must attach a CSV file, silly"]]
                :teams-file [[v/required :message "Please attach a CSV of teams"]
                             [v/matches #".+(\.csv)$" :message "You must attach a CSV file, silly"]])))

(register-handler
  :post-tournament
  (fn [db _]
    (POST "/ajax/tournaments"
          {:headers       {:x-csrf-token (:x-csrf-token db)}
           :body          (js/FormData. (sel1 :#new-tournament-form))
           :handler       #(dispatch! "#/")
           :error-handler #(dispatch [:form-error-resp :name "Unable to process request to server."])})
    db))

(register-handler
  :submit-new-tournament
  (fn [db _]
    (let [validate-data (validate-form)]
      (if-let [e (first validate-data)]
        (assoc db :errors e)
        (do (dispatch [:post-tournament])
            db)))))