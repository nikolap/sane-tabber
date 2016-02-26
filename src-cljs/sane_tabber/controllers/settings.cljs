(ns sane-tabber.controllers.settings
  (:require [ajax.core :refer [POST]]
            [reagent.session :as session]
            [sane-tabber.session :refer [app-state get-by-id insert!]]
            [sane-tabber.utils :refer [id-value]]
            [sane-tabber.controllers.generic :refer [error-handler]]
            [dommy.core :as dom :refer-macros [sel1]]))

(defn unused-users []
  (let [tournament (:tournament @app-state)]
    (filter #(not (contains? (set (conj (:editors tournament) (:owner-id tournament))) (:_id %)))
            (:users @app-state))))

(defn add-editor [username]
  (when (contains? (set (map :username (unused-users))) username)
    (POST (str "/ajax/tournaments/" (session/get :tid) "/editors/add")
          {:headers         {:x-csrf-token (id-value :#__anti-forgery-token)}
           :params          {:id (:_id (get-by-id :users username :username))}
           :handler         #(insert! :tournament %)
           :error-handler   error-handler
           :response-format :transit})
    (dom/set-value! (sel1 :#new-user-form) nil)))

(defn remove-editor [id]
  (POST (str "/ajax/tournaments/" (session/get :tid) "/editors/remove")
        {:headers         {:x-csrf-token (id-value :#__anti-forgery-token)}
         :params          {:id id}
         :handler         #(insert! :tournament %)
         :error-handler   error-handler
         :response-format :transit}))