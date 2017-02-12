(ns sane-tabber.pairings.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [sane-tabber.tooltip.views :refer [tooltip]]
            [sane-tabber.generic.data :refer [get-by-id]]
            [sane-tabber.generic.views :refer [select-custom-form-element]]
            [sane-tabber.generic.utils :refer [format-team-name]]
            [sane-tabber.utils :refer [event-value]]))

(defn judge-tooltip-submit [select-value round-room-id new-judges]
  (let [judges (subscribe [:judges])]
    (when-let [judge (get-by-id @judges select-value :_id)]
      (if round-room-id
        (dispatch [:add-rr-judge round-room-id select-value])
        (swap! new-judges conj judge))
      (dispatch [:set-tooltip-data nil]))))

(defn teams-select [new? teams & [params]]
  (let [stats (subscribe [:stats])
        schools (subscribe [:schools])
        show-stats? (subscribe [:show-stats?])]
    (fn [new? teams & [params]]
      [:select
       (merge {:class (str (if new? "new-team-select" "team-select") " form-control input-sm")} params)
       [:option nil]
       (for [{:keys [_id] :as team} teams
             :let [stats (get-by-id @stats _id :id)]]
         ^{:key _id}
         [:option {:value _id}
          (str (format-team-name team @schools)
               " (" (:points stats) ")"
               (when @show-stats?
                 (str " / (" (clojure.string/join ", " (sort-by first (:position-data stats))) ")")))])])))

(defn judge-label [{:keys [_id name] :as judge} & [removal-coll rr-id params]]
  [:span.tag.label.label-primary
   (merge {} params)
   [:span name]
   [:a>i.remove.glyphicon.glyphicon-remove-sign.glyphicon-white
    {:on-click #(if removal-coll
                 (swap! removal-coll disj judge)
                 (dispatch [:remove-rr-judge rr-id _id]))}]])

(defn pairings-head [{:keys [team-count settings]}]
  [:thead>tr
   [:th "Room"]
   (for [i (range team-count)]
     ^{:key i}
     [:th (or (nth (:position-names settings) i)
              (str "Team " (inc i)))])
   [:th "Judges"]])

(defn pairings-footer [tournament]
  (let [unused-rooms (subscribe [:unused-rooms])
        unused-judges (subscribe [:unused-judges])
        unused-teams (subscribe [:unused-teams])]
    (fn [tournament]
      (let [new-round-judges (reagent/atom #{})]
        [:tr
         [:td [select-custom-form-element "new-rr-room" nil (conj @unused-rooms nil) :name :_id]]

         (for [i (range (:team-count tournament))]
           ^{:key i}
           [:td [teams-select true (sort-by (juxt :school-id :team-code) @unused-teams)]])

         [:td
          (for [{:keys [_id] :as judge} @new-round-judges]
            ^{:key _id}
            [judge-label judge new-round-judges])
          [:button.btn.btn-success.btn-xs
           {:type     "button"
            :on-click #(dispatch [:set-tooltip-data {:left      (.-pageX %)
                                                     :top       (.-pageY %)
                                                     :items     @unused-judges
                                                     :new-items new-round-judges
                                                     :header    "Judge"}])}
           [:i.fa.fa-plus]]

          [:button.btn.btn-primary.btn-xs.btn-flat.pull-right
           {:type     "button"
            :on-click #(dispatch [:submit-new-team-pairings new-round-judges])}
           "Add Room"
           [:i.fa.fa-plus]]]]))))

(defn pairings-table []
  (let [tournament (subscribe [:tournament])
        round-rooms (subscribe [:round-rooms])
        unused-judges (subscribe [:unused-judges])
        rooms (subscribe [:rooms])
        all-judges (subscribe [:judges])]
    (fn []
      [:table.table.table-striped.table-condensed.table-hover
       [pairings-head @tournament]
       [:tbody
        (for [{:keys [_id room judges teams] :as rr} @round-rooms
              :let [room (get-by-id @rooms room :_id)
                    rr-judges (map #(get-by-id @all-judges % :_id) judges)
                    unused-rooms (subscribe [:unused-rooms (:_id room)])]]
          ^{:key _id}
          [:tr
           [:td [select-custom-form-element nil nil @unused-rooms :name :_id
                 {:on-change #(dispatch [:ws-update rr (event-value %) :room :round-rooms])
                  :value     (:_id room)}]]

           (for [i (range (:team-count @tournament))
                 :let [team-id (->> teams
                                    (filter #(= (second %) (inc i)))
                                    first
                                    first
                                    (#(if (nil? %1) %1 (name %1))))
                       unused-teams (subscribe [:unused-teams team-id])]]
             ^{:key i}
             [:td
              [teams-select false (sort-by (juxt :school-id :team-code) @unused-teams)
               {:on-change (fn [e]
                             (dispatch [:ws-update rr
                                        (assoc
                                          (into {}
                                                (filter #(not= (inc i) (second %)) (:teams rr)))
                                          (event-value e) (inc i)) :teams :round-rooms]))
                :value     team-id}]])

           (let [rr-id _id]
             [:td (for [{:keys [_id] :as judge} rr-judges]
                    ^{:key _id}
                    [judge-label judge nil rr-id])
              [:button.btn.btn-success.btn-xs
               {:type     "button"
                :on-click #(dispatch [:set-tooltip-data {:left        (.-pageX %)
                                                         :top         (.-pageY %)
                                                         :items       @unused-judges
                                                         :parent-item _id
                                                         :header      "Judge"}])}
               [:i.fa.fa-plus]]])])
        [pairings-footer @tournament]]])))

(defn unused-pane []
  (let [unused-teams (subscribe [:unused-teams])
        unused-judges (subscribe [:unused-judges])
        schools (subscribe [:schools])
        stats (subscribe [:stats])]
    (fn []
      [:div.box-body
       [:div
        [:h4 "Teams"]
        [:ul
         (for [{:keys [_id] :as team} @unused-teams
               :let [stats (get-by-id @stats _id :id)]]
           ^{:key _id}
           [:li (str (format-team-name team @schools) " (" (:points stats) ")")])]]
       [:div
        [:h4 "Judges"]
        [:ul
         (for [{:keys [_id name]} @unused-judges]
           ^{:key _id}
           [:li name])]]])))

(defn pairings-page []
  (let [tooltip-data (subscribe [:tooltip-data])
        tid (subscribe [:active-tournament])
        rid (subscribe [:active-round])
        tournament (subscribe [:tournament])
        rooms (subscribe [:rooms])
        teams (subscribe [:teams])
        judges (subscribe [:judges])]
    (fn []
      [:section.content>div.row
       [:div.col-sm-8
        [tooltip @tooltip-data judge-tooltip-submit]
        [:div.box.box-primary
         [:div.box-header.with-border>h3.box-title "Pairings"]
         [:a.btn.btn-primary.btn-flat
          {:href   (str "/tournaments/" @tid "/reports/rounds/" @rid "/round-pairings")
           :target "_new"}
          "Export"]
         ;; maybe todo... or think of a better way to do it
         #_[:button.btn.btn-info.btn-flat
          {:type     "button"
           :on-click #(dispatch [:toggle-show-stats])}
          "Toggle Stats"]
         [:div.box-body.no-padding
          (if (and @tournament @rooms @teams @judges)
            [pairings-table]
            [:div.overlay>i.fa.fa-refresh.fa-spin])]]]
       [:div.col-sm-4
        [:div.box.box-info
         [:div.box-header.with-border>h3.box-title "Unused info"]
         [unused-pane]]]])))