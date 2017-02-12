(ns sane-tabber.navigation.views
  (:require [re-frame.core :refer [subscribe]]))

(defn nav-link [uri title icon page active-page]
  [:li {:class (when (= page active-page) "active")}
   [:a {:href uri}
    [:i.fa {:class icon}]
    [:span title]]])

(defn sidebar []
  (let [active-page (subscribe [:active-page])
        active-tournament (subscribe [:active-tournament])]
    (fn []
      (if (contains? #{:home :new-tournament} @active-page)
       [:ul.sidebar-menu
        [:li.header "NAVIGATION"]
        [nav-link "#/" "Home" "fa-home" :home @active-page]
        [nav-link "#/new" "New Tournament" "fa-magic" :new-tournament @active-page]]
       (let [tid @active-tournament]
         [:ul.sidebar-menu
          [:li.header "TOURNAMENT"]
          [nav-link (str "#/" tid "/dashboard") "Dashboard" "fa-dashboard" :dashboard @active-page]
          [nav-link (str "#/" tid "/registration") "Registration" "fa-clipboard" :registration @active-page]
          [nav-link (str "#/" tid "/registration-view") "Registration Review" "fa-tv" :registration-view @active-page]
          [nav-link (str "#/" tid "/pairings") "Pairings" "fa-group" :pairings @active-page]
          [nav-link (str "#/" tid "/ballots") "Ballots" "fa-gavel" :ballots @active-page]
          [nav-link (str "#/" tid "/reporting") "Reporting" "fa-newspaper-o" :reporting @active-page]
          [:li.treeview
           {:class (when (contains? #{:room-editor :judge-editor :team-editor :school-editor} @active-page)
                     "active")}
           [:a {:href "javascript:void(0);"} [:i.fa.fa-edit] [:span "Editors"] [:i.fa.fa-angle-left.pull-right]]
           [:ul.treeview-menu
            [nav-link (str "#/" tid "/editor/rooms") "Room Editor" "fa-bank" :room-editor @active-page]
            [nav-link (str "#/" tid "/editor/judges") "Judge Editor" "fa-balance-scale" :judge-editor @active-page]
            [nav-link (str "#/" tid "/editor/teams") "Team Editor" "fa-child" :team-editor @active-page]
            [nav-link (str "#/" tid "/editor/schools") "School Editor" "fa-industry" :school-editor @active-page]
            ]]
          [nav-link (str "#/" tid "/settings") "Settings" "fa-cogs" :settings @active-page]])))))