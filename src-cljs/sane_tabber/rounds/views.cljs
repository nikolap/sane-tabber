(ns sane-tabber.rounds.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [sane-tabber.utils :refer [dispatch!]]))

(defn rounds-table []
  (let [tid (subscribe [:active-tournament])
        rounds (subscribe [:rounds])]
    (fn []
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
              "partial" [:span.badge.bg-yellow "Partially paired"]
              "paired" [:span.badge.bg-green "Paried"]
              [:span.badge.bg-red "Unpaired"])]
           [:td>div.btn-group
            [:button.btn.btn-success.btn-xs.btn-flat
             {:type     "button"
              :on-click #(dispatch [:auto-pair-click _id status :auto-pair-round])}
             "Auto Pair"]
            [:button.btn.btn-info.btn-xs.btn-flat
             {:type     "button"
              :on-click #(dispatch [:auto-pair-click _id status :auto-pair-judges])}
             "Auto Pair (JUDGES FIRST)"]
            [:button.btn.btn-info.btn-xs.btn-flat
             {:type     "button"
              :disabled (not= "partial" status)
              :on-click #(dispatch [:auto-pair-click _id status :auto-pair-teams])}
             "Auto Pair (ADD TEAMS)"]
            [:button.btn.btn-primary.btn-xs.btn-flat
             {:type     "button"
              :on-click #(dispatch! (str "#/" @tid "/pairings/" _id))}
             "View Rooms"]
            [:button.btn.btn-danger.btn-xs.btn-flat
             {:type     "button"
              :on-click #(dispatch [:delete-round round])}
             "Delete Round"]]])]
       [:tfooter>tr
        [:td
         {:col-span "3"}
         [:button.btn.btn-success.btn-flat.btn-block
          {:type     "button"
           :on-click #(dispatch [:create-round])}
          "Create round"]]]])))

(defn rounds-page []
  [:section.content>div.row>div.col-sm-8
   [:div.box.box-primary
    [:div.box-header.with-border>h3.box-title "Rounds"]
    [:div.box-body.no-padding
     [rounds-table]]]])