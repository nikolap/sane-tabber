(ns sane-tabber.views.pairings
  (:require [reagent.core :as reagent]
            [sane-tabber.session :refer [app-state get-by-id]]
            [sane-tabber.utils :refer [id-value]]
            [sane-tabber.views.tooltip :refer [tooltip tooltip-data hide-tooltip]]
            [sane-tabber.views.generic :refer [select-custom-form-element removable-label]]
            [sane-tabber.controllers.pairings :refer [unused-judges]]))

(defn get-school [id]
  (get-by-id :schools id :_id))

(defn school-name [id]
  (:name (get-school id)))

(defn teams-select [id teams]
  [:select.form-control.input-sm
   {:id id}
   [:option nil]
   (for [{:keys [_id school-id team-code]} teams]
     ^{:key _id}
     [:option {:value _id} (str (school-name school-id) " " team-code)])])

(defn judge-tooltip-submit [select-value round-room-id new-judges]
  (when-let [judge (get-by-id :judges select-value :_id)]
    (if round-room-id
      (prn "asdf")
      (swap! new-judges conj judge))
    (hide-tooltip)))

(defn judge-label [{:keys [name] :as judge} & [removal-coll params]]
  [:span.tag.label.label-primary
   (merge {} params)
   [:span name]
   [:a>i.remove.glyphicon.glyphicon-remove-sign.glyphicon-white
    {:on-click #(if removal-coll
                 (swap! removal-coll disj judge)
                 (prn "asdf"))}]])

(defn pairings-head [{:keys [team-count settings]}]
  [:thead>tr
   [:th "Room"]
   (prn team-count (type team-count))
   (for [i (range team-count)]
       ^{:key i}
       [:th (or (nth (:position-names settings) i)
                (str "Team " (inc i)))])
   [:th "Judges"]])

(defn pairings-footer [tournament round-rooms rooms teams judges]
  (let [new-round-judges (reagent/atom #{})]
    (fn [tournament round-rooms rooms teams judges]
      [:tr
       [:td [select-custom-form-element "new-rr-room" nil (conj rooms nil) :name :_id]]
       (for [i (range (:team-count tournament))]
         ^{:key i}
         [:td [teams-select (str "new-team-" i) (sort-by (juxt :school-id :team-code) teams)]])
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
          :on-click #(prn "asdf")}
         "Add Room"
         [:i.fa.fa-plus]]]])))

(defn pairings-table [{:keys [tournament round-rooms rooms teams judges scratches]}]
  [:table.table.table-striped.table-condensed.table-hover
   [pairings-head tournament]
   [:tbody
    (for [rr round-rooms]
      ^{:key rr}
      [:tr
       [:td "n"]])
    [pairings-footer tournament round-rooms rooms teams judges]]])

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