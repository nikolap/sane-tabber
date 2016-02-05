(ns sane-tabber.views.editors.teams
  (:require [reagent.core :as reagent]
            [sane-tabber.session :refer [app-state get-by-id get-multi]]
            [sane-tabber.utils :refer [event-value id-value]]
            [sane-tabber.controllers.editors.teams :refer [update-accessible update-signed-in update-team-code
                                                           update-school submit-new-team unused-speakers
                                                           update-speaker-team update-speaker-name submit-new-speaker]]
            [sane-tabber.views.generic :refer [input-editor-cell select-custom-form-element input-form-element
                                               checkbox]]))
(defonce tooltip-data (reagent/atom nil))

(def tooltip-x-offset 364)
(def tooltip-y-offset 128)

(defn tooltip [{:keys [left top speakers team-id new-speakers]}]
  (if (and left top)
    [:div.tooltipq {:style {:left (- left tooltip-x-offset)
                            :top  (- top tooltip-y-offset)}}
     [:label.control-label "Speaker"]
     [:button.btn.btn-default.btn-xs.btn-flat.pull-right
      {:type     "button"
       :on-click #(reset! tooltip-data nil)}
      [:i.fa.fa-times]]
     [:select#speaker-select.form-control.input-sm
      [:option nil]
      (for [{:keys [_id name]} speakers]
        ^{:key _id}
        [:option {:value _id} name])]
     [:button.btn.btn-success.btn-xs.btn-flat
      {:type     "button"
       :on-click #(let [speaker (get-by-id :speakers (id-value :#speaker-select) :_id)]
                   (when (not-empty speaker)
                     (if team-id
                       (update-speaker-team speaker team-id)
                       (swap! new-speakers conj speaker))
                     (reset! tooltip-data nil)))}
      [:i.fa.fa-plus] "Add"]]
    [:div]))

(defn speaker-option-prepper [coll]
  (for [{:keys [_id name]} coll]
    {:value _id :label name}))

(defn speaker-label [{:keys [name] :as speaker} & [params]]
  [:span.tag.label.label-primary
   (merge {} params)
   [:span name]
   [:a>i.remove.glyphicon.glyphicon-remove-sign.glyphicon-white
    {:on-click #(update-speaker-team speaker nil)}]])

(defn teams-footer [schools speakers max-speaks]
  (let [new-team-speakers (reagent/atom [])]
    (fn [schools speakers max-speaks]
      [:tfooter>tr
       (prn max-speaks @new-team-speakers)
       [:td [select-custom-form-element "new-team-school-id" nil schools :name :_id]]
       [:td [input-form-element "new-team-code" "text" nil false {:class "input-sm form-control"}]]
       [:td
        (for [{:keys [_id] :as speaker} @new-team-speakers]
          ^{:key _id}
          [speaker-label speaker])
        #_(when (< (count @new-team-speakers) max-speaks)
            [:button.btn.btn-success.btn-xs.pull-right
             {:type     "button"
              :on-click #(reset! tooltip-data {:left         (.-pageX %)
                                               :top          (.-pageY %)
                                               :speakers     (unused-speakers speakers (map :_id @new-team-speakers))
                                               :new-speakers new-team-speakers})}
             [:i.fa.fa-plus]])]
       [:td [checkbox {:id "new-team-accessible"}]]
       [:td [:div.checkbox
             [:label>input {:type "checkbox" :id "new-team-signed-in"}]
             [:button.btn.btn-xs.btn-success.pull-right
              {:type     "button"
               :on-click submit-new-team}
              [:i.fa.fa-plus] " Add Team"]]]])))

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
            [speaker-label speaker])
          (when (< (count team-speakers) max-speaks)
            [:button.btn.btn-success.btn-xs.pull-right
             {:type     "button"
              :on-click #(reset! tooltip-data {:left     (.-pageX %)
                                               :top      (.-pageY %)
                                               :speakers (unused-speakers speakers)
                                               :team-id  _id})}
             [:i.fa.fa-plus]])]
         [:td [:button.btn.btn-xs.btn-flat.btn-block
               {:class    (if accessible? "btn-success" "btn-default")
                :on-click #(update-accessible team)}
               (if accessible? "Accessible" "Not Accessible")]]
         [:td [:button.btn.btn-xs.btn-flat.btn-block
               {:class    (if signed-in? "btn-primary" "btn-danger")
                :on-click #(update-signed-in team)}
               (if signed-in? "In Use" "Not In Use")]]]))
    [teams-footer schools speakers (:speak-count tournament)]]])

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

(defn teams-editor-page []
  [:section.content>div.row>div.col-sm-12
   [tooltip @tooltip-data]
   [:div.box.box-primary
    [:div.box-header.with-border>h3.box-title "Teams"]
    [:div.box-body.no-padding
     [teams-table @app-state]]]
   [:div.box.box-primary
    [:div.box-header.with-border>h3.box-title "Speakers"]
    [:div.box-body.no-padding
     [speakers-table @app-state]]]])