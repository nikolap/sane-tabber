(ns sane-tabber.controllers.new-tournament
  (:require [reagent.core :refer [atom]]
            [dommy.core :refer-macros [sel1]]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [ajax.core :refer [POST]]
            [secretary.core :as secretary]
            [sane-tabber.utils :refer [id-value dispatch!]]))

(defonce errors (atom nil))

(defn error-handler [_]
  (reset! errors {:name ["Something bad happened... you probably goofed up with one of the templates, please check your work"]}))

(defn post-tournament []
  (POST "/ajax/tournaments"
        {:headers       {:x-csrf-token (id-value :#__anti-forgery-token)}
         :body          (js/FormData. (sel1 :#new-tournament-form))
         :handler       #(dispatch! "#/")
         :error-handler error-handler}))

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

(defn submit-new-tournament []
  (let [validate-data (validate-form)]
    (if-let [e (first validate-data)]
      (reset! errors e)
      (post-tournament))))