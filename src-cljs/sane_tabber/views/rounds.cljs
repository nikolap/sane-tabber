(ns sane-tabber.views.rounds
  (:require [reagent.session :as session]
            [sane-tabber.session :refer [app-state]]
            [sane-tabber.utils :refer [dispatch!]]
            [sane-tabber.controllers.rounds :refer [create-round! delete-round! auto-pair-round auto-pair-click]]))

;; show status -- whether round is paired? Is it tabbed (scores entered)?
; automatically pair
; manually pair

(defn rounds-table [{:keys [rounds]}]
  [:table.table.table-striped.table-condensed.table-hover
   [:thead>tr
    [:th "Round"]
    [:th "Status"]
    [:th "Actions"]]
   [:tbody
    (for [{:keys [_id round-number status] :as round} (sort-by :round-number rounds)]
      ^{:key _id}
      [:tr
       [:td round-number]
       [:td
        (case status
          "paired" [:span.badge.bg-yellow "Paired"]
          "complete" [:span.badge.bg-green "Complete"]
          [:span.badge.bg-red "Unpaired"])]
       [:td>div.btn-group
        [:button.btn.btn-success.btn-xs.btn-flat
         {:type     "button"
          :on-click #(auto-pair-click _id status)}
         "Auto Pair"]
        [:button.btn.btn-primary.btn-xs.btn-flat
         {:type "button"
          :on-click #(dispatch! (str "#/" (session/get :tid) "/pairings/" _id))}
         "View Rooms"]
        [:button.btn.btn-danger.btn-xs.btn-flat
         {:type     "button"
          :on-click #(delete-round! round)}
         "Delete Round"]]])]
   [:tfooter>tr
    [:td
     {:col-span "3"}
     [:button.btn.btn-success.btn-flat.btn-block
      {:type     "button"
       :on-click create-round!}
      "Create round"]]]])

(defn rounds-page []
  [:section.content>div.row>div.col-sm-8
   [:div.box.box-primary
    [:div.box-header.with-border>h3.box-title "Rounds"]
    [:div.box-body.no-padding
     [rounds-table @app-state]]]])