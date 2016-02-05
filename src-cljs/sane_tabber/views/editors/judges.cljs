(ns sane-tabber.views.editors.judges
  (:require [reagent.core :as reagent]
            [sane-tabber.session :refer [app-state get-by-id]]
            [sane-tabber.controllers.editors.judges :refer [update-name update-rating update-accessible update-dropped
                                                            submit-new-judge submit-scratch send-scratch]]
            [sane-tabber.views.generic :refer [checkbox select-custom-form-element input-editor-cell]]
            [sane-tabber.utils :refer [id-value event-value]]))

(defn format-team-name [{:keys [school-id team-code]}]
  (str (:name (get-by-id :schools school-id :_id)) " " team-code))

(defn judge-table-footer []
  [:tr
   [:td
    [:div#name-group.form-group.col-xs-12
     [:label#name-error.control-label.hidden "Please enter a name"]
     [:input#new-name.form-control.input-sm
      {:type        "text"
       :placeholder "enter a judge name and press enter"
       :on-key-down #(when (= (.-keyCode %) 13)
                      (submit-new-judge))}]]]
   [:td [:select#new-rating.form-control.input-sm
         {:default-value 5}
         (for [i (range 1 11)]
           ^{:key i}
           [:option i])]]
   [:td [checkbox {:id "new-accessible"}]]
   [:td [:div.checkbox
         [:label>input {:type "checkbox" :id "new-disabled"}]
         [:button.btn.btn-xs.btn-success.pull-right
          {:on-click submit-new-judge}
          [:i.fa.fa-plus] " Add Judge"]]]])

(defn judges-table [judges]
  [:table.table.table-striped.table-condensed.table-hover.table-bordered.table-centered.table-fixed
   [:thead>tr
    [:th "Judge"]
    [:th "Rating"]
    [:th "Accessible?"]
    [:th "Signed in?"]]
   [:tbody
    (for [{:keys [_id name rating accessible? dropped?] :as judge} (sort-by :name judges)]
      ^{:key _id}
      [:tr
       [input-editor-cell judge :name update-name]
       [:td
        [:select.form-control.input-sm
         {:value     rating
          :on-change #(update-rating judge (event-value %))}
         (for [i (range 1 11)]
           ^{:key i}
           [:option i])]]
       [:td [:button.btn.btn-xs.btn-flat.btn-block
             {:class    (if accessible? "btn-success" "btn-default")
              :on-click #(update-accessible judge)}
             (if accessible? "Accessible" "Not Accessible")]]
       [:td [:button.btn.btn-xs.btn-flat.btn-block
             {:class    (if dropped? "btn-primary" "btn-danger")
              :on-click #(update-dropped judge)}
             (if dropped? "In Use" "Not In Use")]]])
    [:tfooter
     [judge-table-footer]]]])

(defn scratches-table-footer [teams judges]
  (let [teams (map #(assoc % :team-name (format-team-name %)) teams)]
    (fn [_ judges]
      [:tr
       [:td
        [select-custom-form-element "new-scratch-judge" nil (sort-by :name judges) :name :_id {:class "input-sm"}]]
       [:td
        [select-custom-form-element "new-scratch-team" nil (sort-by :team-name teams) :team-name :_id {:class "input-sm"}]]
       [:td>button.btn.btn-xs.btn-success
        {:on-click submit-scratch}
        [:i.fa.fa-plus] " Add Scratch"]])))

(defn scratches-table [{:keys [scratches teams judges]}]
  [:table.table.table-striped.table-condensed.table-hover
   [:thead>tr
    [:th "Judge"]
    [:th "Team"]
    [:th ""]]
   [:tbody
    (doall
      (for [{:keys [_id judge-id team-id] :as scratch} scratches]
        ^{:key _id}
        [:tr
         [:td (:name (get-by-id :judges judge-id :_id))]
         [:td (format-team-name (get-by-id :teams team-id :_id))]
         [:td [:button.btn.btn-danger.btn-xs
               {:type     "button"
                :on-click #(send-scratch scratch)}
               "Remove scratch"]]]))
    [:tfooter
     [scratches-table-footer teams judges]]]])

(defn judges-editor-page []
  [:section.content>div.row>div.col-sm-12
   [:div.box.box-primary
    [:div.box-header.with-border>h3.box-title "Judges"]
    [:div.box-body.no-padding
     [judges-table (:judges @app-state)]]]

   [:div.box.box-danger
    [:div.box-header.with-border>h3.box-title "Scratches"]
    [:div.box-body.no-padding
     [scratches-table @app-state]]]])