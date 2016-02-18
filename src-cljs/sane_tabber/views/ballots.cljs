(ns sane-tabber.views.ballots
  (:require [reagent.core :as reagent]
            [sane-tabber.utils :refer [event-value]]
            [sane-tabber.session :refer [app-state get-by-id get-multi]]
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
    [:th "Status"]
    [:th "Actions"]]
   [:tbody
    (for [{:keys [_id room judges teams ballot]} round-rooms
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
       [:td (if ballot [:span.badge.bg-green "Ballot entered"] [:span.badge.bg-red "No ballot"])]
       [:td
        (if ballot
          [:button.btn.btn-danger.btn-xs "Clear Ballot"]
          [:button.btn.btn-success.btn-xs
           {:data-toggle "modal"
            :data-target "#ballot-modal"
            :on-click    #(swap! app-state assoc :active-round-room _id)}
           "Add Ballot"])]])]])

(defn ballot-modal [{:keys [active-round-room]}]
  [:div#ballot-modal.modal.fade>div.modal-dialog>div.modal-content
   [:div.modal-header
    [:button.close {:aria-label "Close" :data-dismiss "modal" :type "button"}]
    [:h4.modal-title "Add Ballot"]]
   [:form#new-board-form
    (let [round-room (get-by-id :round-rooms active-round-room :_id)
          room (get-by-id :rooms (:room round-room) :_id)
          rr-teams (map #(get-by-id :teams (name (first %)) :_id) (sort-by val (:teams round-room)))
          rr-judges (clojure.string/join " || " (map #(:name (get-by-id :judges % :_id)) (:judges round-room)))]
      [:div.modal-body
       [:p [:strong "Room: "] (:name room)]
       [:p [:strong "Judges: "] rr-judges]
       [:h4 "Scores"]
       (for [{:keys [_id] :as team} rr-teams
             :let [speakers (get-multi :speakers _id :team-id)
                   scores (reagent/atom {})]]
         ^{:key _id}
         [:div
          [:p>strong (team-name team)]
          (for [{:keys [_id name]} speakers]
            ^{:key _id}
            [:div.form-group.col-sm-6
             [:label name]
             [:input.form-control.input-sm
              {:id           (str _id "-score")
               :on-change    #(swap! scores assoc _id (js/parseInt (event-value %)))
               :on-key-press (fn [e]
                               (let [cc (.-charCode e)]
                                 (and (>= cc 48)
                                      (<= cc 57))))}]])
          [:div.form-group.col-sm-12
           [:label "Total score"]
           [:input.form-control.input-sm
            {:value    (apply + (vals @scores))
             :disabled true}]]])])
    [:div.modal-footer
     [:button.btn.btn-default.pull-left {:data-dismiss "modal" :type "button"} "Close"]
     [:button.btn.btn-primary {:type     "button"
                               :on-click #(prn "asdf")} "Submit Ballot"]]]])

(defn ballots-page []
  [:section.content>div.row
   [:div.col-sm-12
    [ballot-modal @app-state]
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Balloting"]
     [:div.box-body
      [round-selection @app-state]
      [ballots-table @app-state]]]]])