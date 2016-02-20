(ns sane-tabber.views.ballots
  (:require [sane-tabber.utils :refer [event-value index-of duplicates?]]
            [sane-tabber.session :refer [app-state get-by-id get-multi]]
            [sane-tabber.views.pairings :refer [team-name]]
            [sane-tabber.controllers.ballots :refer [on-change-round! submit-ballot! clear-ballot!]]))

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
    (doall
      (for [i (range (:team-count tournament))]
        ^{:key i}
        [:th (or (nth (get-in tournament [:settings :position-names]) i)
                 (str "Team " (inc i)))]))
    [:th "Judges"]
    [:th "Status"]
    [:th "Actions"]]
   [:tbody
    (doall
      (for [{:keys [_id room judges teams ballot] :as rr} round-rooms
            :let [room (get-by-id :rooms room :_id)
                  rr-teams (map #(get-by-id :teams (name (first %)) :_id) (sort-by val teams))
                  rr-judges (clojure.string/join " || " (map #(:name (get-by-id :judges % :_id)) judges))]]
        ^{:key _id}
        [:tr
         [:td (:name room)]
         (doall
           (for [{:keys [_id] :as team} rr-teams]
             ^{:key _id}
             [:td (team-name team)]))
         [:td rr-judges]
         [:td (if ballot [:span.badge.bg-green "Ballot entered"] [:span.badge.bg-red "No ballot"])]
         [:td
          (if ballot
            [:button.btn.btn-danger.btn-xs
             {:type "button"
              :on-click #(clear-ballot! rr)}
             "Clear Ballot"]
            [:button.btn.btn-success.btn-xs
             {:data-toggle "modal"
              :data-target "#ballot-modal"
              :on-click    #(swap! app-state assoc :active-round-room _id)}
             "Add Ballot"])]]))]])

(defn get-team-score [active-scores team-id]
  (apply + (vals (get active-scores team-id))))

(defn get-team-points [active-scores team-id]
  (index-of (map first (sort-by #(apply + (second %)) > active-scores)) team-id))

(defn ballot-modal [{:keys [tournament active-round-room active-scores]}]
  [:div#ballot-modal.modal.fade>div.modal-dialog>div.modal-content
   [:div
    [:div.modal-header
     [:button.close {:aria-label "Close" :data-dismiss "modal" :type "button"}]
     [:h4.modal-title "Add Ballot"]]
    (let [round-room (get-by-id :round-rooms active-round-room :_id)
          room (get-by-id :rooms (:room round-room) :_id)
          rr-teams (map #(get-by-id :teams (name (first %)) :_id) (sort-by val (:teams round-room)))
          rr-judges (clojure.string/join " || " (map #(:name (get-by-id :judges % :_id)) (:judges round-room)))]
      [:form#new-board-form
       [:div.modal-body
        [:p [:strong "Room: "] (:name room)]
        [:p [:strong "Judges: "] rr-judges]
        [:h4 "Scores"]
        (doall
          (for [{:keys [_id] :as team} rr-teams
                :let [speakers (get-multi :speakers _id :team-id)
                      team-id _id]]
            ^{:key _id}
            [:div
             [:p>strong (team-name team)]
             (doall
               (for [{:keys [_id name]} speakers]
                 ^{:key _id}
                 [:div.form-group.col-sm-6
                  [:label name]
                  [:input.form-control.input-sm
                   {:id           (str _id "-score")
                    :value        (get-in active-scores [team-id _id])
                    :on-change    (fn [e] (swap! app-state assoc-in [:active-scores team-id _id] (js/parseInt (event-value e))))
                    :on-key-press (fn [e]
                                    (let [cc (.-charCode e)]
                                      (and (>= cc 48)
                                           (<= cc 57))))}]]))
             [:div.form-group.col-sm-12
              [:label "Total score"]
              [:input.form-control.input-sm
               {:value    (get-team-score active-scores _id)
                :disabled true}]]]))]
       [:div.modal-footer
        [:button.btn.btn-default.pull-left
         {:data-dismiss "modal"
          :type         "button"
          :on-click     #(swap! app-state assoc :active-scores {} :active-round-room nil)} "Close"]
        [:button.btn.btn-primary
         {:type     "button"
          :on-click (fn []
                      (cond
                        (not= (count (mapcat vals (vals active-scores)))
                              (* (:team-count tournament) (:speak-count tournament))) (js/alert "Please enter ALL speakers scores")
                        (duplicates? (map (comp (partial apply +) vals) (vals active-scores))) (js/alert "Please ensure there are no ties")
                        :else (submit-ballot! round-room
                                              {:teams    (apply merge
                                                                (map #(let [team-id (:_id %)]
                                                                       {team-id {:points (get-team-points active-scores team-id)
                                                                                 :score  (get-team-score active-scores team-id)}})
                                                                     rr-teams))
                                               :speakers (apply merge (vals active-scores))})))}
         "Submit Ballot"]]])]])

(defn ballots-page []
  [:section.content>div.row
   [:div.col-sm-12
    [ballot-modal @app-state]
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Balloting"]
     [:div.box-body
      [round-selection @app-state]
      [ballots-table @app-state]]]]])