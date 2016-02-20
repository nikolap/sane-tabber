(ns sane-tabber.pairings
  (:require [sane-tabber.statistics :refer [judge-seen-teams team-points team-speaks team-position-counts]]))

;; ----Potential logic for auto-pairing----
;; 1. pair teams based off of score
;; 2. assign position based off of least full position usage
;; 3. assign rooms based off of accessible vs not accessible [x]
;; 4. assign judges based off of rating (high judge rating = high rooms). Accessible judges in accessible rooms [x]
;; 5. prioritize minimizing judges seeing the same teams repeatedly [x]

(defn base-group-teams [teams]
  (->> teams
       shuffle
       (partition 2)))

(defn team-map [[t1 t2]]
  {:teams {t1 1
           t2 2}})

(defn teams-accessible? [rr]
  (->> rr
       :teams
       keys
       (some :accessible?)))

(defn sort-accessible-round-rooms [round-rooms]
  (reverse (sort-by teams-accessible? round-rooms)))

(defn room-assigner [rooms round-rooms]
  (map (fn [rr room]
         (assoc rr :room room))
       (sort-accessible-round-rooms round-rooms)
       (reverse (sort-by :accessible? rooms))))

(defn team-ids [round-room]
  (map :_id (keys (:team round-room))))

(defn rr-filter [scratch-team-id judge round-room]
  (and
    (or (not (:accessible? judge))
        (and (:accessible? judge)
             (get-in round-room [:room :accessible?])))
    (not (contains? (set (team-ids round-room)) scratch-team-id))))

(defn rr-judge-filter [round-rooms scratches judge]
  (let [scratch (:team-id (filter #(= (:judge-id %) (:_id judge)) scratches))]
    (filter (partial rr-filter scratch judge) round-rooms)))

(defn update-rr [round-rooms old new]
  (conj (filter (partial not= old) round-rooms) new))

(defn judge-seen-count [prev-round-data judge round-rooms]
  (map #(assoc % :judge-seen-count
                 (judge-seen-teams prev-round-data (:_id judge) (keys (:teams %))))
       round-rooms))

(defn assign-judge-round-room [round-rooms scratches judge prev-round-data]
  (dissoc
    (->> (rr-judge-filter round-rooms scratches judge)
         (judge-seen-count prev-round-data judge)
         (sort-by (juxt :judge-seen-count (comp count :judges)))
         first)
    :judge-seen-count))

(defn judge-looper [judges scratches prev-round-data round-rooms]
  (loop [round-rooms round-rooms
         judge (first judges)
         judges (rest judges)]
    (if judge
      (let [rr (assign-judge-round-room round-rooms scratches judge prev-round-data)]
        (recur (update-rr round-rooms rr (update rr :judges conj judge)) (first judges) (rest judges)))
      round-rooms)))

(defn apply-pull-ups [grouped-teams]
  (if (every? even? (map (comp count second) grouped-teams))
    grouped-teams
    (loop [test-group (first grouped-teams)
           next-group (second grouped-teams)
           rest-groups (rest grouped-teams)
           out []]
      (if next-group
        (if (-> test-group second count even?)
          (recur next-group
                 (first rest-groups)
                 (rest rest-groups)
                 (conj out test-group))
          (recur (update next-group 1 rest)
                 (first rest-groups)
                 (rest rest-groups)
                 (conj out (update test-group 1 conj (-> next-group second first)))))
        out))))

(defn team-id-keyword [team]
  (-> team :_id str keyword))

(defn group-teams [teams prev-round-data]
  (->> teams
       (map #(assoc % :total-points (team-points prev-round-data (team-id-keyword %))
                      :total-speaks (team-speaks prev-round-data (team-id-keyword %))))
       (sort-by :total-speaks >)
       (group-by :total-points)
       (sort-by first >)))

(defn pair-teams [team-groups]
  (->> team-groups
       (mapcat (fn [[_ teams]]
                 (let [[c1 c2] (split-at (/ (count teams) 2) teams)]
                   (interleave c1 (reverse c2)))))
       (partition 2)))

(defn team-roles [prev-round-data teams]
  (apply merge
         (map #(into {}
                     {(str (:_id %)) (->> % team-id-keyword (team-position-counts prev-round-data))})
              teams)))

(defn determine-roles [team-role-data]
  (let [max-role (count team-role-data)]
    (loop [out {}
           teams team-role-data
           current-role 1]
      (if (<= current-role max-role)
        (let [team-for-role (first (sort-by second (map #(into {} {(first %) (get (second %) current-role)}) teams)))]
          (recur
            (assoc out (first (first team-for-role)) current-role)
            (dissoc teams (first (first team-for-role)))
            (inc current-role)))
        {:teams out}))))

(defn assign-least-full-role [team-pairings prev-round-data]
  (map (fn [teams]
         (determine-roles (team-roles prev-round-data teams))) team-pairings))

(defn bracket-teams [teams prev-round-data]
  (-> teams
      (group-teams prev-round-data)
      apply-pull-ups
      pair-teams
      (assign-least-full-role prev-round-data)))

(defn idify-teams [tmap]
  (if (string? (first (keys tmap)))
    tmap
    (let [teams (map (comp :_id first) (sort-by val tmap))]
      (apply merge (map-indexed #(into {} {(str %2) (inc %1)}) teams)))))

(defn idify [round-rooms]
  (map (fn [rr]
         (-> rr
             (update :teams idify-teams)
             (update :room :_id)
             (update :judges #(map :_id %))))
       round-rooms))

(defn pair-round [teams judges rooms scratches prev-round-data]
  (let [paired-teams (if (not-empty prev-round-data)
                       (bracket-teams teams prev-round-data)
                       (map team-map (base-group-teams teams)))]
    (->> paired-teams
         (room-assigner rooms)
         (judge-looper (sort-by :rating > judges) scratches prev-round-data)
         idify)))

(defn pair-judges-only [judges rooms]
  )

(defn pair-teams-to-existing-rooms [teams scratches prev-round-data round-rooms]
  )