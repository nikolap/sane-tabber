(ns sane-tabber.reporting.views
  (:require [re-frame.core :refer [subscribe]]))

(defn reporting-page []
  (let [tid (subscribe [:active-tournament])]
    (fn []
      [:section.content>div.row
       [:div.col-sm-12
        [:div.box.box-primary
         [:div.box-header.with-border>h3.box-title "Download Reports"]
         [:div.box-body
          [:a.btn.btn-primary.btn-flat.btn-block
           {:href   (str "/tournaments/" @tid "/reports/team-tab")
            :target "_blank"}
           "Team Tab"]
          [:a.btn.btn-primary.btn-flat.btn-block
           {:href   (str "/tournaments/" @tid "/reports/speaker-tab")
            :target "_blank"}
           "Speaker Tab"]
          [:a.btn.btn-info.btn-flat.btn-block
           {:href   (str "/tournaments/" @tid "/reports/team-stats")
            :target "_blank"}
           "Team Position Info"]
          [:a.btn.btn-info.btn-flat.btn-block
           {:href   (str "/tournaments/" @tid "/reports/teams")
            :target "_blank"}
           "Teams"]
          [:a.btn.btn-info.btn-flat.btn-block
           {:href   (str "/tournaments/" @tid "/reports/judges")
            :target "_blank"}
           "Judges"]
          [:button.btn.btn-info.btn-flat.btn-block.disabled "Speakers"]
          [:button.btn.btn-info.btn-flat.btn-block.disabled "Schools"]
          [:button.btn.btn-info.btn-flat.btn-block.disabled "Rooms"]]]]])))