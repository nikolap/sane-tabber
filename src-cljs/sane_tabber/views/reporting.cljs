(ns sane-tabber.views.reporting
  (:require [reagent.session :as session]))

(defn reporting-page []
  [:section.content>div.row
   [:div.col-sm-12
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Download Reports"]
     [:div.box-body
      [:a.btn.btn-primary.btn-flat.btn-block
       {:href (str "/tournaments/" (session/get :tid) "/reports/team-tab")
        :target "_blank"}
       "Team Tab"]
      [:a.btn.btn-primary.btn-flat.btn-block
       {:href (str "/tournaments/" (session/get :tid) "/reports/speaker-tab")
        :target "_blank"}
       "Speaker Tab"]
      [:button.btn.btn-info.btn-flat.btn-block.disabled "Team Position Info"]
      [:button.btn.btn-info.btn-flat.btn-block.disabled "Teams"]
      [:button.btn.btn-info.btn-flat.btn-block.disabled "Judges"]
      [:button.btn.btn-info.btn-flat.btn-block.disabled "Speakers"]
      [:button.btn.btn-info.btn-flat.btn-block.disabled "Schools"]
      [:button.btn.btn-info.btn-flat.btn-block.disabled "Rooms"]]]]])