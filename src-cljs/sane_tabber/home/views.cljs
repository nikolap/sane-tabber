(ns sane-tabber.home.views
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn home-header []
  [:section.content-header
   [:h1 "SaneTabber Home"]
   [:ol#breadcrumb.breadcrumb
    [:li.active [:i.fa.fa-home] [:span " "] "Home"]]])

(defn tournaments-table [tournaments]
  [:table.table.table-striped.table-condensed.table-hover
   [:thead>tr
    [:th "Tournament"]
    [:th "Actions"]]
   [:tbody
    (for [{:keys [_id name owner?]} tournaments]
      ^{:key _id}
      [:tr
       [:td name]
       [:td>div.btn-group
        [:a.btn.btn-xs.btn-primary.btn-flat
         {:href (str "#/" _id "/dashboard")}
         "View"]
        (when owner?
          [:button.btn.btn-xs.btn-danger.btn-flat
           {:type     "button"
            :on-click #(dispatch [:delete-tournament _id])}
           "Delete"])]])]])

(defn home-body []
  (let [tournaments (subscribe [:tournaments])]
    (fn []
      [:section.content>div.row>div.col-sm-6>div.box.box-primary
       [:div.box-header.with-border
        [:h3.box-title "Tournaments"]
        [:div.box-tools.pull-right>a.btn.btn-sm.btn-primary.btn-flat {:href "#/new"} "Create New Tournament"]]
       [:div.box-body
        (if (empty? @tournaments)
          [:p "Create a tournament to begin, or talk to your tabs director to gain access to a tournament"]
          [tournaments-table @tournaments])]])))

(defn home-page []
  [:div
   [home-header]
   [home-body]])