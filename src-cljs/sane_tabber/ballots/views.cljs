(ns sane-tabber.ballots.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [sane-tabber.generic.data :refer [get-by-id get-multi]]
            [sane-tabber.generic.utils :refer [format-team-name]]
            [sane-tabber.utils :refer [event-value]]
            [sane-tabber.ballots.handlers :refer [get-team-score]]))

(defn round-selection []
  (let [rounds (subscribe [:rounds])
        active-round (subscribe [:active-round])]
    (fn []
      [:div.form-group.col-sm-4.col-sm-offset-8
       [:label "Round selection"]
       [:select.form-control.input-sm
        {:on-change #(dispatch [:on-change-round (event-value %)])
         :value     @active-round}
        [:option nil]
        (for [{:keys [_id round-number]} (filter #(= "paired" (:status %)) @rounds)]
          ^{:key _id}
          [:option {:value _id} round-number])]])))

(defn ballots-table []
  (let [tournament (subscribe [:tournament])
        round-rooms (subscribe [:round-rooms])
        schools (subscribe [:schools])
        rooms (subscribe [:rooms])
        all-teams (subscribe [:teams])
        all-judges (subscribe [:judges])]
    (fn []
      [:table.table.table-striped.table-bordered.table-condensed.table-hover.table-padded.table-centered
       [:thead>tr
        [:th "Room"]
        (doall
          (for [i (range (:team-count @tournament))]
            ^{:key i}
            [:th (or (nth (get-in @tournament [:settings :position-names]) i)
                     (str "Team " (inc i)))]))
        [:th "Judges"]
        [:th "Status"]
        [:th "Actions"]]
       [:tbody
        (doall
          (for [{:keys [_id room judges teams ballot] :as rr} @round-rooms
                :let [room (get-by-id @rooms room :_id)
                      rr-teams (map #(get-by-id @all-teams (name (first %)) :_id) (sort-by val teams))
                      rr-judges (clojure.string/join " || " (map #(:name (get-by-id @all-judges % :_id)) judges))]]
            ^{:key _id}
            [:tr
             [:td (:name room)]
             (doall
               (for [{:keys [_id] :as team} rr-teams]
                 ^{:key _id}
                 [:td (format-team-name team @schools)]))
             [:td rr-judges]
             [:td (if ballot [:span.badge.bg-green "Ballot entered"] [:span.badge.bg-red "No ballot"])]
             [:td
              (if ballot
                [:button.btn.btn-danger.btn-xs
                 {:type     "button"
                  :on-click #(dispatch [:clear-ballot rr])}
                 "Clear Ballot"]
                [:button.btn.btn-success.btn-xs
                 {:data-toggle "modal"
                  :data-target "#ballot-modal"
                  :on-click    #(dispatch [:set-active-round-room _id])}
                 "Add Ballot"])]]))]])))

(defn ballot-modal []
  (let [active-scores (subscribe [:active-scores])
        active-round-room (subscribe [:active-round-room])
        tournament (subscribe [:tournament])
        schools (subscribe [:schools])
        rooms (subscribe [:rooms])
        round-rooms (subscribe [:round-rooms])
        teams (subscribe [:teams])
        judges (subscribe [:judges])
        speakers (subscribe [:speakers])]
    (fn []
      [:div#ballot-modal.modal.fade>div.modal-dialog>div.modal-content
       [:div
        [:div.modal-header
         [:button.close {:aria-label "Close" :data-dismiss "modal" :type "button"}]
         [:h4.modal-title "Add Ballot"]]
        (let [round-room (get-by-id @round-rooms @active-round-room :_id)
              room (get-by-id @rooms (:room round-room) :_id)
              rr-teams (map #(get-by-id @teams (name (first %)) :_id) (sort-by val (:teams round-room)))
              rr-judges (clojure.string/join " || " (map #(:name (get-by-id @judges % :_id)) (:judges round-room)))]
          [:form#new-board-form
           {:on-key-down #(when (= (.-keyCode %) 13)
                           (dispatch [:submit-ballot @active-scores @tournament round-room rr-teams]))}
           [:div.modal-body
            [:p [:strong "Room: "] (:name room)]
            [:p [:strong "Judges: "] rr-judges]
            [:h4 "Scores"]
            (doall
              (for [{:keys [_id] :as team} rr-teams
                    :let [speakers (get-multi @speakers _id :team-id)
                          team-id _id]]
                ^{:key _id}
                [:div
                 [:p>strong (format-team-name team @schools)]
                 (doall
                   (for [{:keys [_id name]} speakers]
                     ^{:key _id}
                     [:div.form-group.col-sm-6
                      [:label name]
                      [:input.form-control.input-sm
                       {:id           (str _id "-score")
                        :value        (get-in @active-scores [team-id _id])
                        :on-change    (fn [e] (dispatch [:update-active-score team-id _id (event-value e)]))
                        :on-key-press (fn [e]
                                        (let [cc (.-charCode e)]
                                          (and (>= cc 48)
                                               (<= cc 57))))}]]))
                 [:div.form-group.col-sm-12
                  [:label "Total score"]
                  [:input.form-control.input-sm
                   {:value    (get-team-score @active-scores _id)
                    :disabled true}]]]))]
           [:div.modal-footer
            [:button.btn.btn-default.pull-left
             {:data-dismiss "modal"
              :type         "button"
              :on-click     #(dispatch [:close-ballot-modal])} "Close"]
            [:button.btn.btn-primary
             {:type     "button"
              :on-click #(dispatch [:submit-ballot @active-scores @tournament round-room rr-teams])}
             "Submit Ballot"]]])]])))

;; todo: exporting
(defn ballots-page []
  [:section.content>div.row
   [:div.col-sm-12
    [ballot-modal]
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Balloting"]
     [:div.box-body
      [round-selection]
      [ballots-table]]]]])