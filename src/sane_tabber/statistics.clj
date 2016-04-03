(ns sane-tabber.statistics
  (:require [sane-tabber.utils :refer [filter-first]]))

(defn ballot-filter [k v]
  (filter #(contains? (-> %
                          (get-in [:ballot k])
                          keys
                          set)
                      v)))

(defn ks-map [& ks]
  (map #(get-in % ks)))

(defn get-by-id [coll id k]
  (filter-first #(= (get % k) id) coll))

(defn team-ballots-filter [team-id]
  (ballot-filter :teams team-id))

(defn teams-ballots-filter [teams-id]
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
    (transduce (comp (teams-ballots-filter teams-id)
                     (mapcat :judges)
                     (filter (partial = judge-id)))
               conj
               round-data)))

(defn team-position-counts [round-data team-id]
  (frequencies
    (transduce (comp (team-ballots-filter team-id)
                     (map #(get-in % [:teams team-id]))) conj round-data)))

(defn pairing-stats [round-data teams]
  (map (fn [{:keys [_id]}]
         {:id            _id
          :points        (team-points round-data (keyword (str _id)))
          :position-data (team-position-counts round-data (keyword (str _id)))}) teams))