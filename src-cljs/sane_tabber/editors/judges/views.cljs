(ns sane-tabber.editors.judges.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [sane-tabber.generic.data :refer [get-by-id]]
            [sane-tabber.generic.views :refer [checkbox select-custom-form-element input-editor-cell]]
            [sane-tabber.generic.utils :refer [format-team-name]]
            [sane-tabber.utils :refer [event-value]]))

(defn judge-table-footer [registration?]
  [:tr
   [:td
    [:div#name-group.form-group.col-xs-12
     [:label#name-error.control-label.hidden "Please enter a name"]
     [:input#new-name.form-control.input-sm
      {:type        "text"
       :placeholder "enter a judge name and press enter"
       :on-key-down #(when (= (.-keyCode %) 13)
                      (dispatch [:submit-judge]))}]]]
   [:td
    {:class (when registration? "hidden")}
    [:select#new-rating.form-control.input-sm
     {:default-value 5}
     (for [i (range 1 11)]
       ^{:key i}
       [:option i])]]
   [:td [checkbox nil {:id "new-accessible"}]]
   [:td [:div.checkbox
         [:label>input {:type "checkbox" :id "new-disabled"}]
         [:button.btn.btn-xs.btn-success.pull-right
          {:on-click #(dispatch [:submit-judge])}
          [:i.fa.fa-plus] " Add Judge"]]]])

(defn judges-table [registration?]
  (let [judges (subscribe [:judges])]
    (fn [registration?]
      [:table.table.table-striped.table-condensed.table-hover.table-bordered.table-centered.table-fixed
       [:thead>tr
        [:th "Judge"]
        (when-not registration? [:th "Rating"])
        [:th "Accessible?"]
        [:th "Signed in?"]]
       [:tbody
        (for [{:keys [_id name rating accessible? signed-in?] :as judge} (sort-by :name @judges)]
          ^{:key _id}
          [:tr
           [input-editor-cell judge :name (fn [judge name] (dispatch [:ws-update judge name :name :judges]))]
           (when-not registration?
             [:td
              [:select.form-control.input-sm
               {:value     rating
                :on-change #(dispatch [:ws-update judge (js/parseInt (event-value %)) :rating :judges])}
               (for [i (range 1 11)]
                 ^{:key i}
                 [:option i])]])
           [:td [:button.btn.btn-xs.btn-flat.btn-block
                 {:class    (if accessible? "btn-success" "btn-default")
                  :on-click #(dispatch [:send-transit-toggle judge :accessible? :judges])}
                 (if accessible? "Accessible" "Not Accessible")]]
           [:td [:button.btn.btn-xs.btn-flat.btn-block
                 {:class    (if signed-in? "btn-primary" "btn-danger")
                  :on-click #(dispatch [:send-transit-toggle judge :signed-in? :judges])}
                 (if signed-in? "In Use" "Not In Use")]]])]
       [:tfoot
        [judge-table-footer registration?]]])))

(defn scratches-table-footer [teams judges schools]
  (fn [teams judges schools]
    (let [teams (map #(assoc % :team-name (format-team-name % schools)) teams)]
      [:tr
       [:td
        [select-custom-form-element "new-scratch-judge" nil (sort-by :name judges) :name :_id {:class "input-sm"}]]
       [:td
        [select-custom-form-element "new-scratch-team" nil (sort-by :team-name teams) :team-name :_id {:class "input-sm"}]]
       [:td>button.btn.btn-xs.btn-success
        {:on-click #(dispatch [:submit-scratch])}
        [:i.fa.fa-plus] " Add Scratch"]])))

(defn scratches-table []
  (let [scratches (subscribe [:scratches])
        teams (subscribe [:teams])
        judges (subscribe [:judges])
        schools (subscribe [:schools])]
    (fn []
      [:table.table.table-striped.table-condensed.table-hover
       [:thead>tr
        [:th "Judge"]
        [:th "Team"]
        [:th ""]]
       [:tbody
        (doall
          (for [{:keys [_id judge-id team-id] :as scratch} @scratches]
            ^{:key _id}
            [:tr
             [:td (:name (get-by-id @judges judge-id :_id))]
             [:td (format-team-name (get-by-id @teams team-id :_id) @schools)]
             [:td [:button.btn.btn-danger.btn-xs
                   {:type     "button"
                    :on-click #(dispatch [:send-scratch scratch])}
                   "Remove scratch"]]]))]
       [:tfoot
        [scratches-table-footer @teams @judges @schools]]])))

(defn judges-editor-page
  ([registration?]
   [:section.content>div.row>div.col-sm-12
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Judges"]
     [:div.box-body.no-padding
      [judges-table registration?]]]

    (when-not registration?
      [:div.box.box-danger
       [:div.box-header.with-border>h3.box-title "Scratches"]
       [:div.box-body.no-padding
        [scratches-table]]])])
  ([]
   [judges-editor-page false]))