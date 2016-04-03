(ns sane-tabber.generic.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch]]
            [sane-tabber.utils :refer [event-value]]))

(defn form-element [id label item & [error]]
  [:div.form-group {:class (when error "has-error")}
   (when error
     [:div.callout.callout-danger
      (first error)])
   (if label [:label {:for id} label])
   item])

(defn input-form-element [id form-type label fc? & [params error]]
  [form-element id label
   [:input (merge {:id   id
                   :name id
                   :class (when fc? "form-control")
                   :type form-type} params)]
   error])

(defn select-form-element [id label option-list & [params]]
  [form-element id label
   [:select.form-control (merge {:id id :name id} params)
    (for [opt option-list]
      ^{:key opt}
      [:option opt])]])

(defn checkbox [& [text params]]
  [:div.checkbox>label
   [:input (merge {:type "checkbox"} params)]
   text])

(defn select-custom-form-element [id label option-list vkey & [opt-vkey params]]
  [form-element id label
   [:select.form-control.input-sm (merge {:id id :name id} params)
    (for [opt option-list]
      ^{:key opt}
      [:option {:value (if opt-vkey (opt-vkey opt) opt)} (vkey opt)])]])

(defn input-editor-cell [item vkey update-fn]
  "Creates a cell with an input and a button. Takes the a map (item), a value key for the map, and an update-fn
  Update-fn dispatches a re-frame handler, which takes the original item map and the new value per for vkey"
  (let [v (reagent/atom (vkey item))]
    (fn [item]
      [:td
       [:div.col-xs-11>input.form-control.input-sm
        {:type        "text"
         :value       @v
         :on-change   #(reset! v (event-value %))
         :on-key-down #(when (= (.-keyCode %) 13)
                        (update-fn item @v))}]
       [:button.btn.btn-success.btn-xs.pull-right
        {:type     "button"
         :on-click #(update-fn item @v)}
        [:i.fa.fa-check]]])))

(defn removable-label [{:keys [name] :as item} update-fn & [removal-coll params]]
  [:span.tag.label.label-primary
   (merge {} params)
   [:span name]
   [:a>i.remove.glyphicon.glyphicon-remove-sign.glyphicon-white
    {:on-click #(if removal-coll
                 (swap! removal-coll disj item)
                 (update-fn))}]])