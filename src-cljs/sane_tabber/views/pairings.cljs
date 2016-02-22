(ns sane-tabber.views.pairings
  (:require [reagent.core :as reagent]
            [sane-tabber.session :refer [app-state get-by-id]]
            [sane-tabber.utils :refer [id-value event-value]]
            [sane-tabber.views.tooltip :refer [tooltip tooltip-data hide-tooltip]]
            [sane-tabber.views.generic :refer [select-custom-form-element removable-label]]
            [sane-tabber.controllers.pairings :refer [unused-rooms unused-judges unused-teams submit-new-team
                                                      update-round-room-room update-round-room-teams
                                                      add-rr-judge remove-rr-judge]]))

(defn get-school [id]
  (get-by-id :schools id :_id))

(defn school-name [id]
  (:name (get-school id)))

(defn team-name [{:keys [school-id team-code]}]
  (str (school-name school-id) " " team-code))

(defn teams-select [new? teams & [params]]
  [:select
   (merge {:class (str (if new? "new-team-select" "team-select") " form-control input-sm")} params)
   (if new? [:option nil] [:option {:disabled true :selected true} "-- select an option -- "])
   (for [{:keys [_id] :as team} teams]
     ^{:key _id}
     [:option {:value _id} (team-name team)])])

(defn judge-tooltip-submit [select-value round-room-id new-judges]
  (when-let [judge (get-by-id :judges select-value :_id)]
    (if round-room-id
      (add-rr-judge round-room-id select-value)
      (swap! new-judges conj judge))
    (hide-tooltip)))

(defn judge-label [{:keys [_id name] :as judge} & [removal-coll rr-id params]]
  [:span.tag.label.label-primary
   (merge {} params)
   [:span name]
   [:a>i.remove.glyphicon.glyphicon-remove-sign.glyphicon-white
    {:on-click #(if removal-coll
                 (swap! removal-coll disj judge)
                 (remove-rr-judge rr-id _id))}]])

(defn pairings-head [{:keys [team-count settings]}]
  [:thead>tr
   [:th "Room"]
   (for [i (range team-count)]
     ^{:key i}
     [:th (or (nth (:position-names settings) i)
              (str "Team " (inc i)))])
   [:th "Judges"]])

(defn pairings-footer [tournament round-rooms rooms teams judges]
  (let [new-round-judges (reagent/atom #{})]
    (fn [tournament round-rooms rooms teams judges]
      [:tr
       [:td [select-custom-form-element "new-rr-room" nil (conj (unused-rooms rooms round-rooms) nil) :name :_id]]

       (for [i (range (:team-count tournament))]
         ^{:key i}
         [:td [teams-select true (sort-by (juxt :school-id :team-code) (unused-teams teams round-rooms))]])

       [:td
        (for [{:keys [_id] :as judge} @new-round-judges]
          ^{:key _id}
          [judge-label judge new-round-judges])
        [:button.btn.btn-success.btn-xs
         {:type     "button"
          :on-click #(reset! tooltip-data {:left      (.-pageX %)
                                           :top       (.-pageY %)
                                           :items     (unused-judges judges round-rooms)
                                           :new-items new-round-judges
                                           :header    "Judge"})}
         [:i.fa.fa-plus]]

        [:button.btn.btn-primary.btn-xs.btn-flat.pull-right
         {:type     "button"
          :on-click #(submit-new-team new-round-judges)}
         "Add Room"
         [:i.fa.fa-plus]]]])))

(defn pairings-table [{:keys [tournament round-rooms rooms teams judges scratches]}]
  [:table.table.table-striped.table-condensed.table-hover
   [pairings-head tournament]
   (let [all-teams teams
         all-judges judges]
     [:tbody
      (for [{:keys [_id room judges teams] :as rr} round-rooms
            :let [room (get-by-id :rooms room :_id)
                  rr-teams (when teams (map #(get-by-id :teams (name (first %)) :_id) (sort-by val teams)))
                  rr-judges (map #(get-by-id :judges % :_id) judges)]]
        ^{:key _id}
        [:tr
         [:td [select-custom-form-element nil nil (unused-rooms rooms round-rooms (:_id room)) :name :_id
               {:on-change #(update-round-room-room rr (event-value %))
                :value     (:_id room)}]]

         (for [i (range (:team-count tournament))
               :let [team-id (:_id (nth rr-teams i))]]
           ^{:key i}
           [:td
            [teams-select false (sort-by (juxt :school-id :team-code) (unused-teams all-teams round-rooms team-id))
             {:on-change #(update-round-room-teams rr (event-value %) (inc i))
              :value     team-id}]])

         (let [rr-id _id]
           [:td (for [{:keys [_id] :as judge} rr-judges]
                  ^{:key _id}
                  [judge-label judge nil rr-id])
            [:button.btn.btn-success.btn-xs
             {:type     "button"
              :on-click #(reset! tooltip-data {:left        (.-pageX %)
                                               :top         (.-pageY %)
                                               :items       (unused-judges all-judges round-rooms)
                                               :parent-item _id
                                               :header      "Judge"})}
             [:i.fa.fa-plus]]])])
      [pairings-footer tournament round-rooms rooms all-teams all-judges]])])

;; todo: export pairings to spreadsheet
(defn pairings-page []
  [:section.content>div.row
   [:div.col-sm-8
    [tooltip @tooltip-data judge-tooltip-submit]
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Pairings"]
     [:div.box-body.no-padding
      (if (every? @app-state [:tournament :rooms :teams :judges])
        [pairings-table @app-state]
        [:div.overlay>i.fa.fa-refresh.fa-spin])]]]
   [:div.col-sm-4
    [:div.box.box-info
     [:div.box-header.with-border>h3.box-title "Unused info"]
     [:div.box-body
      ]]]])