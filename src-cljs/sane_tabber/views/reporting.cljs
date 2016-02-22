(ns sane-tabber.views.reporting)

(defn reporting-page []
  [:section.content>div.row
   [:div.col-sm-12
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Download Reports"]
     [:div.box-body
      [:button.btn.btn-primary.btn-flat.btn-block "Team Tab"]
      [:button.btn.btn-primary.btn-flat.btn-block "Speaker Tab"]
      [:button.btn.btn-primary.btn-flat.btn-block "Team Stats/Info"]
      [:button.btn.btn-primary.btn-flat.btn-block.disabled "Teams"]
      [:button.btn.btn-primary.btn-flat.btn-block.disabled "Judges"]
      [:button.btn.btn-primary.btn-flat.btn-block.disabled "Speakers"]
      [:button.btn.btn-primary.btn-flat.btn-block.disabled "Schools"]
      [:button.btn.btn-primary.btn-flat.btn-block.disabled "Rooms"]]]]])