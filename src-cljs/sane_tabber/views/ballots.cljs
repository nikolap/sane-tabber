(ns sane-tabber.views.ballots
  (:require [sane-tabber.utils :refer [event-value]]
            [sane-tabber.session :refer [app-state get-by-id]]
            [sane-tabber.views.pairings :refer [team-name]]
            [sane-tabber.controllers.ballots :refer [on-change-round!]]))

(defn round-selection [{:keys [rounds active-round]}]
  [:div.form-group.col-sm-4.col-sm-offset-8
   [:label "Round selection"]
   [:select.form-control.input-sm
    {:on-change on-change-round!
     :value     active-round}
    [:option nil]
    (for [{:keys [_id round-number]} rounds]
      ^{:key _id}
      [:option {:value _id} round-number])]])

(defn ballots-table [{:keys [round-rooms tournament]}]
  [:table.table.table-striped.table-bordered.table-condensed.table-hover.table-padded.table-centered
   [:thead>tr
    [:th "Room"]
    (for [i (range (:team-count tournament))]
      ^{:key i}
      [:th (or (nth (get-in tournament [:settings :position-names]) i)
               (str "Team " (inc i)))])
    [:th "Judges"]
    [:th "Actions"]]
   [:tbody
    (for [{:keys [_id room judges teams]} round-rooms
          :let [room (get-by-id :rooms room :_id)
                rr-teams (map #(get-by-id :teams (name (first %)) :_id) (sort-by val teams))
                rr-judges (clojure.string/join " || " (map #(:name (get-by-id :judges % :_id)) judges))]]
      ^{:key _id}
      [:tr
       [:td (:name room)]
       (for [{:keys [_id] :as team} rr-teams]
         ^{:key _id}
         [:td (team-name team)])
       [:td rr-judges]
       [:td>div.btn-group
        [:button.btn.btn-success.btn-xs "Add Ballot"]
        [:button.btn.btn-danger.btn-xs "Clear Ballot"]]])]])

(defn ballots-page []
  [:section.content>div.row
   [:div.col-sm-12
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Balloting"]
     [:div.box-body
      [round-selection @app-state]
      [ballots-table @app-state]]]]])