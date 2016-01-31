(ns sane-tabber.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[sane-tabber started successfully]=-"))
   :middleware identity})
