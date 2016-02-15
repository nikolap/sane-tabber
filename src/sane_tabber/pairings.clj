(ns sane-tabber.pairings)

;; ----Potential logic for auto-pairing----
;; 1. pair teams based off of score
;; 2. assign position based off of least full position usage
;; 3. assign rooms based off of accessible vs not accessible
;; 4. assign judges based off of rating (high judge rating = high rooms). Accessible judges in accessible rooms
;; 5. prioritize minimizing judges seeing the same teams repeatedly

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
  (sort-by teams-accessible? round-rooms))

(defn room-assigner [rooms round-rooms]
  (map (fn [rr room]
         (assoc rr :room room))
       (sort-accessible-round-rooms round-rooms)
       (sort-by :accessible? rooms)))

(defn team-ids [round-room]
  (map :_id (keys (:team round-room))))

(defn rr-filter [scratch-team-id judge round-room]
  (if (or (and (:accessible? judge)
               (not (get-in round-room [:room :accesible?])))
          (contains? (set (team-ids round-room)) scratch-team-id))
    false
    true))

(defn rr-judge-filter [round-rooms scratches judge]
  (let [scratch (:team-id (filter #(= (:judge-id %) (:_id judge)) scratches))]
    (filter (partial rr-filter scratch judge) round-rooms)))

(defn update-rr [round-rooms old new]
  (conj (filter (partial not= old) round-rooms) new))

(defn base-judge-looper [judges scratches round-rooms]
  (loop [round-rooms round-rooms
         judge (first judges)
         judges (rest judges)]
    (if (empty? judges)
      round-rooms
      (let [rr (->> (rr-judge-filter round-rooms scratches judge)
                    (sort-by #(count (:judges %)))
                    first)]
        (recur (update-rr round-rooms rr (update rr :judges conj judge)) (first judges) (rest judges))))))

(defn idify-teams [tmap]
  (let [[t1 t2] (map :_id (keys tmap))]
    {(str t1) 1
     (str t2) 2}))

(defn idify [round-rooms]
  (map (fn [rr]
         (-> rr
             (update :teams idify-teams)
             (update :room :_id)
             (update :judges #(map :_id %))))
       round-rooms))

(defn initial-pairings [teams judges rooms scratches]
  (let [paired-teams (base-group-teams teams)]
    (->> paired-teams
         (map team-map)
         (room-assigner rooms)
         (base-judge-looper (sort-by :rating > judges) scratches)
         idify)))

(defn pair-round [teams judges rooms scratches prev-round-data]
  (if (empty? prev-round-data)
    (initial-pairings teams judges rooms scratches)
    ))