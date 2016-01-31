(ns sane-tabber.views.editors.teams
  (:require [sane-tabber.session :refer [app-state get-by-id]]))

(defn teams-table [{:keys [teams]}]
  [:table.table.table-striped.table-condensed.table-hover
   [:thead>tr
    [:th "School"]
    [:th "Code"]
    [:th "Speakers"]
    [:th "Accessible?"]
    [:th "Signed in?"]]
   [:tbody
    (for [{:keys [_id team-code school-id accessible? signed-in?]} (sort-by (juxt :school-id :team-code) teams)
          :let [school (get-by-id :schools school-id :_id)]]
      ^{:key _id}
      [:tr
       [:td (:name school)]
       [:td team-code]
       [:td "TODO"]
       [:td accessible?]
       [:td signed-in?]])
    [:tfooter>tr
     [:td "ASDF"]]]])

(defn teams-editor-page []
  [:section.content>div.row>div.col-sm-12>div.box.box-primary
   [:div.box-header.with-border
    [:h3.box-title "Teams"]]
   [:div.box-body.no-padding
    [teams-table @app-state]]])