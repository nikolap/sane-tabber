(ns sane-tabber.views.home
  (:require [ajax.core :refer [DELETE]]
            [sane-tabber.session :refer [app-state]]
            [sane-tabber.utils :refer [id-value reload]]))

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
            :on-click #(if (js/confirm "Are you sure you wish to delete this tournament? No backsies!")
                        (DELETE (str "/ajax/tournaments/" _id "/delete")
                                {:headers {:x-csrf-token (id-value :#__anti-forgery-token)}
                                 :handler reload}))}
           "Delete"])]])]])

(defn home-body []
  [:section.content>div.row>div.col-sm-6>div.box.box-primary
   [:div.box-header.with-border
    [:h3.box-title "Tournaments"]
    [:div.box-tools.pull-right>a.btn.btn-sm.btn-primary.btn-flat {:href "#/new"} "Create New Tournament"]]
   [:div.box-body
    (if (empty? (:tournaments @app-state))
      [:p "Create a tournament to begin, or talk to your tabs director to gain access to a tournament"]
      [tournaments-table (:tournaments @app-state)])]])

(defn home-page []
  [:div
   [home-header]
   [home-body]])