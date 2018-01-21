(ns sane-tabber.editors.rooms.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [dommy.core :as dom :refer-macros [sel1]]
            [sane-tabber.generic.views :refer [checkbox]]
            [sane-tabber.utils :refer [id-value]]))

(defn new-room [value tid]
  (let [room {:name          (id-value :#new-name)
              :accessible?   (.-checked (sel1 :#new-accessible))
              :disabled?     (.-checked (sel1 :#new-disabled))
              :tournament-id tid}]
    (if (clojure.string/blank? (:name room))
      (do
        (dom/add-class! (sel1 :#name-group) "has-error")
        (dom/remove-class! (sel1 :#name-error) "hidden"))
      (do
        (dispatch [:create-room room])
        (dom/remove-class! (sel1 :#name-group) "has-error")
        (dom/add-class! (sel1 :#name-error) "hidden")
        (reset! value (-> @value
                          (clojure.string/split " ")
                          first
                          (str " ")))
        (dom/set-attr! (sel1 :#new-accessible) :checked false)
        (dom/set-attr! (sel1 :#new-disabled) :checked false)))))

(defn room-table-footer [tid]
  (let [value (reagent/atom "")]
    (fn []
      [:tr
       [:td
        [:div#name-group.form-group
         [:label#name-error.control-label.hidden "Please enter a name"]
         [:input.form-control.input-sm
          {:type        "text"
           :id          "new-name"
           :placeholder "enter a room name and press enter"
           :value       @value
           :on-change   #(reset! value (-> % .-target .-value))
           :on-key-down #(when (= (.-keyCode %) 13)
                          (new-room value tid))}]]]
       [:td [checkbox nil {:id "new-accessible"}]]
       [:td [:div.checkbox
             [:label>input {:type "checkbox" :id "new-disabled"}]
             [:button.btn.btn-xs.btn-success.pull-right
              {:on-click #(new-room value tid)}
              [:i.fa.fa-plus] " Add Room"]]]])))

(defn rooms-table []
  (let [rooms (subscribe [:rooms])
        tid (subscribe [:active-tournament])]
    (fn []
     [:table.table.table-striped.table-condensed.table-hover.table-bordered.table-centered.table-fixed
      [:thead>tr
       [:th "Room"]
       [:th "Accessible?"]
       [:th "Do not use?"]]
      [:tbody
       (for [{:keys [_id name accessible? disabled?] :as room} (sort-by :name @rooms)]
         ^{:key _id}
         [:tr
          [:td name]
          [:td [:button.btn.btn-xs.btn-flat.btn-block
                {:class    (if accessible? "btn-success" "btn-default")
                 :on-click #(dispatch [:send-transit-toggle room :accessible?])}
                (if accessible? "Accessible" "Not Accessible")]]
          [:td [:button.btn.btn-xs.btn-flat.btn-block
                {:class    (if disabled? "btn-danger" "btn-primary")
                 :on-click #(dispatch [:send-transit-toggle room :disabled?])}
                (if disabled? "Not In Use" "In Use")]]])]
      [:tfoot
       [room-table-footer @tid]]])))

(defn rooms-editor-page []
  [:section.content>div.row>div.col-sm-12>div.box.box-primary
   [:div.box-header.with-border
    [:h3.box-title "Rooms"]]
   [:div.box-body.no-padding
    [rooms-table]]])