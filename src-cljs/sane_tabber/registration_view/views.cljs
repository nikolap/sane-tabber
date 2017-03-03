(ns sane-tabber.registration-view.views
  (:require [re-frame.core :refer [subscribe]]
            [sane-tabber.generic.data :refer [get-by-id get-multi]]
            [sane-tabber.generic.utils :refer [format-team-name]]))

(defn team-table []
  (let [teams (subscribe [:teams])
        speakers (subscribe [:speakers])
        schools (subscribe [:schools])]
    (fn []
      [:div
       {:style {:position "fixed"
                :left "0px"
                :top "0px"
                :z-index 99999
                :height "100%"
                :background-color "#FFF"
                :overflow-y "scroll"}}
       [:h3 "If you see yourselves on this list as not signed in, please visit the registration desk as soon as possible."]

       [:table.table.table-striped.table-condensed.table-hover.table-fixed
        [:thead>tr
         [:th "Team"]
         [:th "Speakers"]
         [:th "Signed in?"]]
        [:tbody
         (doall
           (for [{:keys [_id signed-in?] :as team} (sort-by (juxt :signed-in? :school-id :team-code) @teams)
                 :let [team-speakers (get-multi @speakers _id :team-id)]]
             ^{:key _id}
             [:tr
              [:td
               (format-team-name team @schools)]

              [:td
               (clojure.string/join ", " (map :name team-speakers))]
              [:td [:div.btn.btn-xs.btn-flat.btn-block
                    {:class (if signed-in? "btn-primary" "btn-danger")}
                    (if signed-in? [:span [:i.fa.fa-check] "  Signed in"] [:span [:i.fa.fa-times] " NOT SIGNED IN"])]]]))]]])))

(defn registration-view-page []
  [:div
   [team-table]])