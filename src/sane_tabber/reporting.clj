(ns sane-tabber.reporting
  (:require [sane-tabber.statistics :refer [team-points team-speaks speaker-score get-by-id team-position-counts]]
            [sane-tabber.csv :refer [write-csv]]
            [sane-tabber.db.core :as db]))

(defn oid->key [id]
  (-> id str keyword))

(defn round-keywords [rounds]
  (mapv (comp keyword (partial str "R") :round-number) rounds))

(defn generate-team-header [rounds]
  (concat [:team-name] (round-keywords rounds) [:total-points :total-speaks]))

(defn generate-speaker-header [rounds]
  (concat [:speaker :team-name] (round-keywords rounds) [:total-speaks :average]))

(defn position-incrementer [team-count]
  (for [i (range 1 (inc team-count))]
    (keyword (str "team-" i))))

(defn generate-pairings-header [team-count]
  (concat [:room] (position-incrementer team-count) [:judges]))

(defn generate-position-stats-header [team-count]
  (concat [:team-name] (position-incrementer team-count)))

(defn round-data-extractor [rounds round-data item-id score-fn]
  (into {}
        (map
          (fn [{:keys [_id round-number]}]
            [(keyword (str "R" round-number))
             (score-fn (filter #(= (:round-id %) _id) round-data) item-id)])
          rounds)))

(defn teams-tab [teams schools round-data rounds]
  (let [header-keys (generate-team-header rounds)
        map-seq (reverse
                  (sort-by (juxt :total-points :total-speaks)
                           (map (fn [{:keys [_id team-code school-id] :as team}]
                                  (let [tid (oid->key _id)]
                                    (-> team
                                        (assoc :team-name (str (:name (get-by-id schools school-id :_id)) " " team-code)
                                               :total-points (team-points round-data tid)
                                               :total-speaks (team-speaks round-data tid))
                                        (merge (round-data-extractor rounds round-data tid team-points)))))
                                teams)))]
    (write-csv map-seq header-keys)))

(defn speakers-tab [speakers teams schools round-data rounds]
  (let [header-keys (generate-speaker-header rounds)
        map-seq (reverse
                  (sort-by (juxt :total-speaks)
                           (map (fn [{:keys [_id team-id name] :as speaker}]
                                  (let [team (get-by-id teams team-id :_id)
                                        speaker-id (oid->key _id)]
                                    (-> speaker
                                        (assoc :speaker name
                                               :team-name (str (:name (get-by-id schools (:school-id team) :_id)) " " (:team-code team))
                                               :total-speaks (speaker-score round-data speaker-id)
                                               :average (double (/ (speaker-score round-data speaker-id) (count rounds))))
                                        (merge (round-data-extractor rounds round-data speaker-id speaker-score)))))
                                speakers)))]
    (write-csv map-seq header-keys)))

(defn team-position-stats [teams schools round-data tournament]
  (let [team-count (:team-count tournament)
        header-keys (generate-position-stats-header team-count)
        map-seq (map (fn [team]
                       (let [pos-counts (team-position-counts round-data (keyword (str (:_id team))))]
                         (apply
                           merge
                           {:team-name (str (:name (get-by-id schools (:school-id team) :_id)) " " (:team-code team))}
                           (map (fn [i]
                                  {(keyword (str "team-" i)) (get pos-counts i)})
                                (range 1 (inc team-count)))))) teams)]
    (write-csv map-seq header-keys)))

(defn round-pairings [rooms all-teams schools all-judges round-data tournament]
  (let [header-keys (generate-pairings-header (:team-count tournament))
        map-seq (map (fn [{:keys [room teams judges]}]
                       (apply
                         merge
                         {:room   (:name (get-by-id rooms room :_id))
                          :judges (clojure.string/join " // " (map #(:name (get-by-id all-judges % :_id)) judges))}
                         (map-indexed (fn [i [id _]]
                                        (let [team (get-by-id all-teams (db/object-id (name id)) :_id)]
                                          {(keyword (str "team-" (inc i)))
                                           (str (:name (get-by-id schools (:school-id team) :_id)) " " (:team-code team))}))
                                      (sort-by second teams))))
                     round-data)]
    (write-csv map-seq header-keys)))

(defn round-ballots [])

(defn export-teams [all-teams all-speakers schools]
  (let [header-keys [:team-name :speakers]
        map-seq (map (fn [team]
                       {:team-name (str (:name (get-by-id schools (:school-id team) :_id)) " " (:team-code team))
                        :speakers  (clojure.string/join "," (map :name (filter #(= (:team-id %) (:_id team)) all-speakers)))})
                     all-teams)]
    (write-csv map-seq header-keys)))

(defn export-judges [])

(defn export-speakers [])

(defn export-schools [])

(defn export-rooms [])