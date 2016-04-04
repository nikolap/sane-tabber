(ns sane-tabber.ballots.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]
            [sane-tabber.generic.subs :refer [basic-get-sub]]))

(basic-get-sub :active-round)
(basic-get-sub :active-scores)
(basic-get-sub :active-round-room)