(ns sane-tabber.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [sane-tabber.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[sane-tabber started successfully using the development profile]=-"))
   :middleware wrap-dev})
