(ns sane-tabber.statistics)

;; Round room structure
;{:judges [ids...]
;   :room   id
;   :teams  {:id position-num
;            :id position-num
;            ...}
;   :ballot {:teams    {:id {:points n
;                            :score  n}}
;            :speakers {:id score}
; :tournament-id id
; :round-id id}}

(def test-data [{:judges [1 2]
                 :teams {1 1
                         2 2}
                 :ballot {:teams    {1 {:points 1 :score 84}
                                       2 {:points 0 :score 78}}
                            :speakers {1 33
                                       2 42
                                       3 62
                                       4 79}}}
                  {:judges [1 2]
                   :teams {1 1
                           2 2}
                   :ballot {:teams    {1 {:points 1 :score 84}
                                       2 {:points 0 :score 78}}
                            :speakers {1 33
                                       2 42
                                       3 62
                                       4 79}}}
                  {:judges [1 2]
                   :teams {1 1
                           2 2}
                   :ballot {:teams    {1 {:points 1 :score 84}
                                       2 {:points 0 :score 78}}
                            :speakers {1 33
                                       2 42
                                       3 62
                                       4 79}}}
                  {:judges [1 2]
                   :teams {1 1
                           2 2}
                   :ballot {:teams    {1 {:points 1 :score 84}
                                       2 {:points 0 :score 78}}
                            :speakers {1 33
                                       2 42
                                       3 62
                                       4 79}}}])

(defn ballot-filter [k v]
  (filter #(contains? (-> %
                          (get-in [:ballot k])
                          keys
                          set)
                      v)))

(defn ks-map [& ks]
  (map #(get-in % ks)))

(defn team-ballots-filter [team-id]
  (ballot-filter :teams team-id))

(defn teams-ballot-filter [teams-id]
  (filter (fn [rd]
            (some (set (keys (:teams rd))) teams-id))))

(defn team-points-map [team-id]
  (ks-map :ballot :teams team-id :points))

(defn team-scores-map [team-id]
  (ks-map :ballot :teams team-id :score))

(defn speaker-ballots-filter [speaker-id]
  (ballot-filter :speakers speaker-id))

(defn speaker-scores-map [speaker-id]
  (ks-map :ballot :speakers speaker-id))

(defn incremental-transducer [coll & transducers]
  (transduce (apply comp transducers) + coll))

(defn team-points [round-data team-id]
  (incremental-transducer round-data (team-ballots-filter team-id) (team-points-map team-id)))

(defn team-speaks [round-data team-id]
  (incremental-transducer round-data (team-ballots-filter team-id) (team-scores-map team-id)))

(defn speaker-score [round-data speaker-id]
  (incremental-transducer round-data (speaker-ballots-filter speaker-id) (speaker-scores-map speaker-id)))

(defn judge-seen-teams [round-data judge-id teams-id]
  (count
    (transduce (comp (teams-ballot-filter teams-id)
                     (mapcat :judges)
                     (filter (partial = judge-id)))
               conj
               round-data)))