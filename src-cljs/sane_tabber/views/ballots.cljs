(ns sane-tabber.views.ballots
  (:require [sane-tabber.session :refer [app-state]]))

(defn ballots-table [{:keys [round-rooms]}]
  [:table.table.table-striped.table-bordered.table-condensed.table-hover.table-padded.table-centered
   "asdf"])

(defn ballots-page []
  [:section.content>div.row
   [:div.col-sm-12
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Rooms"]
     [:div.box-body.no-padding
      [ballots-table @app-state]]]]])