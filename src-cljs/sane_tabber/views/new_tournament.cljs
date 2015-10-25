(ns sane-tabber.views.new-tournament
  (:require [sane-tabber.views.generic :refer [input-form-element select-form-element]]
            [sane-tabber.controllers.new-tournament :refer [submit-new-tournament errors]]))

(defn new-tournament-header []
  [:section.content-header
   [:h1 "Create New Tournament"]
   [:ol#breadcrumb.breadcrumb
    [:li>a {:href "#/"} [:i.fa.fa-home] "Home"]
    [:li.active [:i.fa.fa-magic] [:span " "] "New Tournament"]]])

(defn new-tournament-form [{:keys [name rooms-file schools-file judges-file teams-file]}]
  [:div.box-body
   [:form#new-tournament-form
    {:enc-type "multipart/form-data"}
    [input-form-element "name" "text" "Tournament Name" true {:placeholder "The Best Tournament IV"} name]
    [select-form-element "team-count" "Number of teams per round" [2 3 4]]
    [select-form-element "speak-count" "Number of speakers per team" [1 2 3] {:value 2}]
    [input-form-element "rooms-file" "file" "Rooms CSV file" false {:accept "csv"} rooms-file]
    [input-form-element "schools-file" "file" "Schools CSV file" false {:accept "csv"} schools-file]
    [input-form-element "judges-file" "file" "Judges CSV file" false {:accept "csv"} judges-file]
    [input-form-element "teams-file" "file" "Teams CSV file" false {:accept "csv"} teams-file]]])

(defn new-tournament-footer []
  [:div.box-footer>button.btn.btn-primary.btn-flat
   {:type     "button"
    :on-click submit-new-tournament}
   "Submit"])

(defn template-box []
  [:div.col-sm-6>div.box.box-default
   [:div.box-header.with-border
    [:h3.box-title "Templates"]]
   [:div.box-body
    [:p "Please use the attached templates as starting points for populating data for your new tournament. It's much
    faster to just edit this data in a spreadsheet than any tool such as this one... so you may as well do that."]
    [:p "If you have more or less speakers per team, you are welcome to add/remove columns as necessary from
    the teams template."]
    [:p "Otherwise, please ensure you adhere to the format in the attached templates... should you fail, SaneTabber
    may ridicule you."]
    [:div.btn-group
     [:a.btn.btn-sm.btn-info.btn-flat {:href "/csv/rooms-template.csv"} "Rooms"]
     [:a.btn.btn-sm.btn-info.btn-flat {:href "/csv/schools-template.csv"} "Schools"]
     [:a.btn.btn-sm.btn-info.btn-flat {:href "/csv/judges-template.csv"} "Judges"]
     [:a.btn.btn-sm.btn-info.btn-flat {:href "/csv/teams-template.csv"} "Teams"]]]])

(defn new-tournament-body []
  [:section.content>div.row
   [:div.col-sm-6>div.box.box-primary
    [:div.box-header.with-border
     [:h3.box-title "New Tournament"]]
    [new-tournament-form @errors]
    [new-tournament-footer]]
   [template-box]])

(defn new-tournament-page []
  [:div
   [new-tournament-header]
   [new-tournament-body]])