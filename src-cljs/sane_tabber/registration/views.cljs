(ns sane-tabber.registration.views
  (:require [sane-tabber.editors.teams.views :refer [teams-editor-page]]
            [sane-tabber.editors.judges.views :refer [judges-editor-page]]))

(defn registration-page []
  [:div
   [teams-editor-page true]
   [judges-editor-page true]])