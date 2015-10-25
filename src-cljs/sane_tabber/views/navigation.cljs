(ns sane-tabber.views.navigation
  (:require [reagent.session :as session]
            [sane-tabber.views.generic :refer [nav-link]]))

(defn sidebar []
  (if (contains? #{:home :new-tournament} (session/get :page))
    [:ul.sidebar-menu
     [:li.header "NAVIGATION"]
     [nav-link "#/" "Home" "fa-home" :home]
     [nav-link "#/new" "New Tournament" "fa-magic" :new-tournament]]
    (let [tid (session/get :tid)]
      [:ul.sidebar-menu
       [:li.header "TOURNAMENT"]
       [nav-link (str "#/" tid "/dashboard") "Dashboard" "fa-dashboard" :dashboard]
       [nav-link (str "#/" tid "/registration") "Registration" "fa-clipboard" :registration]
       [nav-link (str "#/" tid "/pairings") "Pairings" "fa-group" :pairings]
       [nav-link (str "#/" tid "/ballots") "Ballots" "fa-gavel" :ballots]
       [nav-link (str "#/" tid "/reporting") "Reporting" "fa-newspaper-o" :reporting]
       [:li.treeview
        {:class (when (contains? #{:room-editor :judge-editor :team-editor :school-editor} (session/get :page))
                  "active")}
        [:a {:href "javascript:void(0);"} [:i.fa.fa-edit] [:span "Editors"] [:i.fa.fa-angle-left.pull-right]]
        [:ul.treeview-menu
         [nav-link (str "#/" tid "/editor/rooms") "Room Editor" "fa-bank" :room-editor]
         [nav-link (str "#/" tid "/editor/judges") "Judge Editor" "fa-balance-scale" :judge-editor]
         [nav-link (str "#/" tid "/editor/teams") "Team Editor" "fa-child" :team-editor]
         [nav-link (str "#/" tid "/editor/schools") "School Editor" "fa-industry" :school-editor]
         ]]
       [nav-link (str "#/" tid "/settings") "Settings" "fa-cogs" :settings]])))