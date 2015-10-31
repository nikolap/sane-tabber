(ns sane-tabber.views.editors.judges
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [dommy.core :as dom :refer-macros [sel1]]
            [sane-tabber.session :refer [app-state]]
            [sane-tabber.controllers.editors.rooms :refer [update-accessible update-disabled create-room]]
            [sane-tabber.views.generic :refer [checkbox]]
            [sane-tabber.utils :refer [id-value]]))

(defn judges-table [judges]
  [:p "asdf"])

(defn scratches-table [{:keys [scratches teams]}]
  [:p "asdf"])

(defn judges-editor-page []
  [:section.content>div.row>div.col-sm-12>div.box.box-primary
   [:div.box-header.with-border
    [:h3.box-title "Rooms"]]
   [:div.box-body.no-padding
    [judges-table (:judges @app-state)]
    [scratches-table @app-state]]])