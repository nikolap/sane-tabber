(ns sane-tabber.generic.views)

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