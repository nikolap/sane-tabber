(ns sane-tabber.controllers.pairings)

(defn unused-judges [judges round-rooms]
  (filter #(not (contains? (set (map :judges round-rooms)) (:_id judges))) judges))