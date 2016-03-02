(ns sane-tabber.views.editors.teams
  (:require [reagent.core :as reagent]
            [sane-tabber.session :refer [app-state get-by-id get-multi]]
            [sane-tabber.utils :refer [event-value id-value]]
            [sane-tabber.controllers.editors.teams :refer [update-accessible update-signed-in update-team-code
                                                           update-school submit-new-team unused-speakers
                                                           update-speaker-team update-speaker-name submit-new-speaker]]
            [sane-tabber.views.generic :refer [input-editor-cell select-custom-form-element input-form-element
                                               checkbox removable-label]]
            [sane-tabber.views.tooltip :refer [tooltip tooltip-data hide-tooltip]]))

(defn speaker-tooltip-fn [select-val team-id new-speakers]
  (when-let [speaker (get-by-id :speakers select-val :_id)]
    (if team-id
      (update-speaker-team speaker team-id)
      (swap! new-speakers conj speaker))
    (hide-tooltip)))

(defn teams-footer [schools]
  [:tfooter>tr
   [:td [select-custom-form-element "new-team-school-id" nil schools :name :_id]]
   [:td [input-form-element "new-team-code" "text" nil false {:class "input-sm form-control"}]]
   [:td nil]
   [:td [checkbox {:id "new-team-accessible"}]]
   [:td [:div.checkbox
         [:label>input {:type "checkbox" :id "new-team-signed-in"}]
         [:button.btn.btn-xs.btn-success.pull-right
          {:type     "button"
           :on-click submit-new-team}
          [:i.fa.fa-plus] " Add Team"]]]])

(defn teams-table [{:keys [teams schools speakers tournament]}]
  [:table.table.table-striped.table-condensed.table-hover.table-fixed
   [:thead>tr
    [:th "School"]
    [:th "Code"]
    [:th "Speakers"]
    [:th "Accessible?"]
    [:th "Signed in?"]]
   [:tbody
    (doall
      (for [{:keys [_id school-id accessible? signed-in?] :as team} (sort-by (juxt :school-id :team-code) teams)
            :let [team-speakers (get-multi :speakers _id :team-id)
                  max-speaks (:speak-count tournament)]]
        ^{:key _id}
        [:tr
         [:td
          [select-custom-form-element nil nil schools :name :_id
           {:on-change #(update-school team (event-value %))
            :value     school-id}]]
         [input-editor-cell team :team-code update-team-code]
         [:td
          (for [{:keys [_id] :as speaker} team-speakers]
            ^{:key _id}
            [removable-label speaker #(update-speaker-team speaker nil)])
          (when (< (count team-speakers) max-speaks)
            [:button.btn.btn-success.btn-xs.pull-right
             {:type     "button"
              :on-click #(reset! tooltip-data {:left        (.-pageX %)
                                               :top         (.-pageY %)
                                               :items       (unused-speakers speakers)
                                               :parent-item _id})}
             [:i.fa.fa-plus]])]
         [:td [:button.btn.btn-xs.btn-flat.btn-block
               {:class    (if accessible? "btn-success" "btn-default")
                :on-click #(update-accessible team)}
               (if accessible? "Accessible" "Not Accessible")]]
         [:td [:button.btn.btn-xs.btn-flat.btn-block
               {:class    (if signed-in? "btn-primary" "btn-danger")
                :on-click #(update-signed-in team)}
               (if signed-in? "In Use" "Not In Use")]]]))
    [teams-footer schools]]])

(defn speakers-table [{:keys [speakers]}]
  [:div.scroll-vertical>table.table.table-striped.table-condensed.table-hover
   [:thead>tr
    [:th "Name"]
    [:th "Assigned?"]]
   [:tbody
    (for [{:keys [_id team-id] :as speaker} (sort-by :_id speakers)]
      ^{:key _id}
      [:tr
       [input-editor-cell speaker :name update-speaker-name]
       [:td
        (if (clojure.string/blank? team-id)
          [:span.badge.bg-red "No"]
          [:span.badge.bg-green "Yes"])]])
    [:tfooter>tr
     [:td [input-form-element "new-speaker-name" "text" nil true
           {:placeholder "Enter new speaker name and press enter"
            :on-key-down #(when (= (.-keyCode %) 13)
                           (submit-new-speaker))}]]
     [:td>button.btn.btn-success.btn-block
      {:type     "button"
       :on-click submit-new-speaker}
      "Add Speaker (or press enter)"]]]])

(defn teams-editor-page
  ([registration?]
   [:section.content>div.row>div.col-sm-12
    [tooltip @tooltip-data speaker-tooltip-fn]
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Teams"]
     [:div.box-body.no-padding
      [teams-table @app-state]]]
    (when-not registration?
      [:div.box.box-primary
       [:div.box-header.with-border>h3.box-title "Speakers"]
       [:div.box-body.no-padding
        [speakers-table @app-state]]])])
  ([]
   [teams-editor-page false]))