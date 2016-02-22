(ns sane-tabber.reporting
  (:require [sane-tabber.statistics :refer [team-points team-speaks speaker-score get-by-id]]
            [sane-tabber.csv :refer [write-csv]]))

(defn oid->key [id]
  (-> id str keyword))

(defn round-keywords [rounds]
  (mapv (comp keyword (partial str "R") :round-number) rounds))

(defn generate-team-header [rounds]
  (concat [:team-name] (round-keywords rounds) [:total-points :total-speaks]))

(defn generate-speaker-header [rounds]
  (concat [:speaker :team-name] (round-keywords rounds) [:total-speaks :average]))

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

(defn team-position-stats [teams round-data rounds])

(defn round-pairings [])

(defn round-ballots [])

(defn export-teams [])

(defn export-judges [])

(defn export-speakers [])

(defn export-schools [])

(defn export-rooms [])