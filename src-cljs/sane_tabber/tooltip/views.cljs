(ns sane-tabber.tooltip.views
  (:require [re-frame.core :refer [dispatch]]
            [reagent.core :as reagent]
            [sane-tabber.utils :refer [event-value]]))

(def tooltip-x-offset 364)
(def tooltip-y-offset 128)

(defn tooltip [{:keys [left top header parent-item items new-items]} on-click-fn]
  (let [select-val (reagent/atom nil)]
    (fn [{:keys [left top header parent-item items new-items]} on-click-handler]
      (if (and left top)
        [:div.tooltipq {:style {:left (- left tooltip-x-offset)
                                :top  (- top tooltip-y-offset)}}
         [:label.control-label header]
         [:button.btn.btn-default.btn-xs.btn-flat.pull-right
          {:type     "button"
           :on-click #(dispatch [:set-tooltip-data nil])}
          [:i.fa.fa-times]]
         [:select#tooltip-select.form-control.input-sm
          {:value     @select-val
           :on-change #(reset! select-val (event-value %))}
          [:option nil]
          (for [{:keys [_id name]} items]
            ^{:key _id}
            [:option {:value _id} name])]
         [:button.btn.btn-success.btn-xs.btn-flat
          {:type     "button"
           :on-click #(on-click-fn @select-val parent-item new-items)}
          [:i.fa.fa-plus] "Add"]]
        [:div]))))