(ns sane-tabber.views.registration
  (:require [sane-tabber.views.editors.judges :refer [judges-editor-page]]
            [sane-tabber.views.editors.teams :refer [teams-editor-page]]))

(defn registration-page []
  [:div
   [teams-editor-page]
   [judges-editor-page]])