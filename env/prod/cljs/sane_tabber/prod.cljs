(ns sane-tabber.app
  (:require [sane-tabber.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
