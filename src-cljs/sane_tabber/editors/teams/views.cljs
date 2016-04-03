(ns sane-tabber.editors.teams.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [sane-tabber.generic.data :refer [get-by-id get-multi]]
            [sane-tabber.generic.views :refer [checkbox select-custom-form-element input-form-element input-editor-cell
                                               removable-label]]
            [sane-tabber.tooltip.views :refer [tooltip]]
            [sane-tabber.utils :refer [event-value]]))

(defn speaker-tooltip-fn [select-val team-id new-speakers]
  (let [speakers (subscribe [:speakers])]
    (when-let [speaker (get-by-id @speakers select-val :_id)]
      (if team-id
        (dispatch [:ws-update speaker team-id :team-id :speakers])
        (swap! new-speakers conj speaker))
      (dispatch [:set-tooltip-data nil]))))

(defn teams-footer [schools]
  [:tfooter>tr
   [:td [select-custom-form-element "new-team-school-id" nil schools :name :_id]]
   [:td [input-form-element "new-team-code" "text" nil false {:class "input-sm form-control"}]]
   [:td nil]
   [:td [checkbox nil {:id "new-team-accessible"}]]
   [:td [:div.checkbox
         [:label>input {:type "checkbox" :id "new-team-signed-in"}]
         [:button.btn.btn-xs.btn-success.pull-right
          {:type     "button"
           :on-click #(dispatch [:submit-new-team])}
          [:i.fa.fa-plus] " Add Team"]]]])

(defn teams-table []
  (let [tournament (subscribe [:tournament])
        teams (subscribe [:teams])
        speakers (subscribe [:speakers])
        schools (subscribe [:schools])
        unused-speakers (subscribe [:unused-speakers])]
    (fn []
      [:table.table.table-striped.table-condensed.table-hover.table-fixed
       [:thead>tr
        [:th "School"]
        [:th "Code"]
        [:th "Speakers"]
        [:th "Accessible?"]
        [:th "Signed in?"]]
       [:tbody
        (doall
          (for [{:keys [_id school-id accessible? signed-in?] :as team} (sort-by (juxt :school-id :team-code) @teams)
                :let [team-speakers (get-multi @speakers _id :team-id)
                      max-speaks (:speak-count @tournament)]]
            ^{:key _id}
            [:tr
             [:td
              [select-custom-form-element nil nil @schools :name :_id
               {:on-change #(dispatch [:ws-update team (event-value %) :school-id :teams])
                :value     school-id}]]
             [input-editor-cell team :team-code (fn [team new-code]
                                                  (dispatch [:ws-update team new-code :team-code :teams]))]
             [:td
              (for [{:keys [_id] :as speaker} team-speakers]
                ^{:key _id}
                [removable-label speaker #(dispatch [:ws-update speaker nil :team-id :speakers])])
              (when (< (count team-speakers) max-speaks)
                [:button.btn.btn-success.btn-xs.pull-right
                 {:type     "button"
                  :on-click #(dispatch [:set-tooltip-data {:left        (.-pageX %)
                                                           :top         (.-pageY %)
                                                           :items       @unused-speakers
                                                           :parent-item _id}])}
                 [:i.fa.fa-plus]])]
             [:td [:button.btn.btn-xs.btn-flat.btn-block
                   {:class    (if accessible? "btn-success" "btn-default")
                    :on-click #(dispatch [:send-transit-toggle team :accessible? :teams])}
                   (if accessible? "Accessible" "Not Accessible")]]
             [:td [:button.btn.btn-xs.btn-flat.btn-block
                   {:class    (if signed-in? "btn-primary" "btn-danger")
                    :on-click #(dispatch [:send-transit-toggle team :signed-in? :teams])}
                   (if signed-in? "In Use" "Not In Use")]]]))
        [teams-footer @schools]]])))

(defn speakers-table []
  (let [speakers (subscribe [:speakers])]
    (fn []
      [:div.scroll-vertical>table.table.table-striped.table-condensed.table-hover
       [:thead>tr
        [:th "Name"]
        [:th "Assigned?"]]
       [:tbody
        (for [{:keys [_id team-id] :as speaker} (sort-by :_id @speakers)]
          ^{:key _id}
          [:tr
           [input-editor-cell speaker :name
            (fn [speaker new-name] (dispatch [:ws-update speaker new-name :name :speakers]))]
           [:td
            (if (clojure.string/blank? team-id)
              [:span.badge.bg-red "No"]
              [:span.badge.bg-green "Yes"])]])
        [:tfooter>tr
         [:td [input-form-element "new-speaker-name" "text" nil true
               {:placeholder "Enter new speaker name and press enter"
                :on-key-down #(when (= (.-keyCode %) 13)
                               (dispatch [:submit-new-speaker]))}]]
         [:td>button.btn.btn-success.btn-block
          {:type     "button"
           :on-click #(dispatch [:submit-new-speaker])}
          "Add Speaker (or press enter)"]]]])))

(defn teams-editor-page
  ([registration?]
   (let [tooltip-data (subscribe [:tooltip-data])]
     (fn [registration?]
       [:section.content>div.row>div.col-sm-12
        [tooltip @tooltip-data speaker-tooltip-fn]
        [:div.box.box-primary
         [:div.box-header.with-border>h3.box-title "Teams"]
         [:div.box-body.no-padding
          [teams-table]]]
        (when-not registration?
          [:div.box.box-primary
           [:div.box-header.with-border>h3.box-title "Speakers"]
           [:div.box-body.no-padding
            [speakers-table]]])])))
  ([]
   [teams-editor-page false]))