(ns sane-tabber.pairings)

;; ----Potential logic for auto-pairing----
;; 1. pair teams based off of score
;; 1b. swap teams who have are from same school, if scores remain same
;; 2. assign position based off of least full position usage
;; 3. assign rooms based off of accessible vs not accessible
;; 4. assign judges based off of rating (high judge rating = high rooms). Accessible judges in accessible rooms
;; 4b. prioritize minimizing judges seeing the same teams repeatedly

(defn pair-round [teams judges rooms prev-round-data]
  )