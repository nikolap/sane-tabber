(ns sane-tabber.settings.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [sane-tabber.generic.data :refer [get-by-id]]
            [sane-tabber.utils :refer [event-value id-value]]))

(defn bloodhound []
  (let [unused-users (subscribe [:unused-users])]
    (js/Bloodhound. (clj->js
                     {:datumTokenizer (-> js/Bloodhound .-tokenizers .-whitespace)
                      :queryTokenizer (-> js/Bloodhound .-tokenizers .-whitespace)
                      :local          (map :username @unused-users)}))))

(defn mount-typeahead []
  (.typeahead (js/$ ".typeahead") "destroy")
  (.typeahead (js/$ ".typeahead")
              (clj->js
                {:hint      true
                 :highlight true
                 :minLength 3})
              (clj->js
                {:name   "new-editor"
                 :source (bloodhound)})))

(defn settings-view []
  (let [tournament (subscribe [:tournament])
        users (subscribe [:users])]
    (fn []
      [:div.box-body
      [:div
       [:h4 "Editors"]
       [:ul
        (if (not-empty (:editors @tournament))
          (for [id (:editors @tournament)
                :let [user (get-by-id @users id :_id)]]
            ^{:key (:username user)}
            [:li
             (:username user)
             [:button.btn.btn-danger.btn-flat.btn-xs
              {:type     "button"
               :on-click #(dispatch [:remove-editor id])}
              [:i.fa.fa-times]]])
          [:li "You may choose to add editors below, otherwise only you can see this tournament"])]
       [:div.row
        [:div.col-sm-10>input#new-user-form.form-control.typeahead
         {:type        "text"
          :placeholder "begin typing to add user"
          :on-key-down #(when (= (.-keyCode %) 13)
                         (dispatch [:add-editor (event-value %)]))}]
        [:div.col-sm-2>button.btn.btn-success.btn-flat.btn-block
         {:type     "button"
          :on-click #(dispatch [:add-editor (id-value :#new-user-form)])}
         [:i.fa.fa-plus]]]
       ]])))

(defn settings-body []
  (reagent/create-class
    {:component-did-mount  mount-typeahead
     :component-did-update mount-typeahead
     :reagent-render       settings-view}))

(defn settings-page []
  [:section.content>div.row
   [:div.col-sm-12
    [:div.box.box-primary
     [:div.box-header.with-border>h3.box-title "Settings"]
     [settings-body]]]])