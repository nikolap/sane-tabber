(ns sane-tabber.routes.tabber
  (:require [compojure.core :refer [defroutes GET POST DELETE context]]
            [ring.util.http-response :refer [ok]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [sane-tabber.layout :as layout]
            [sane-tabber.db.core :as db]
            [sane-tabber.utils :refer [stringify-reduce stringify-map wrap-transit-resp]]))

(defn app-page []
  (layout/render "home.html"))

(defn read-csv-file [file]
  (-> file :tempfile slurp csv/read-csv rest))

(defn upload-rooms [tournament-id rooms-file]
  (let [data (->> rooms-file read-csv-file (map #(into {} {:name (first %) :tournament-id tournament-id})))]
    (db/batch-create-rooms data)))

(defn upload-schools [tournament-id schools-file]
  (let [data (->> schools-file
                  read-csv-file
                  (map #(into {}
                              {:name          (first %)
                               :code          (second %)
                               :tournament-id tournament-id})))]
    (db/batch-create-schools data)))

(defn upload-judges [tournament-id judges-file]
  (let [data (->> judges-file
                  read-csv-file
                  (map #(into {}
                              {:name          (first %)
                               :rating        (Integer/parseInt (second %))
                               :tournament-id tournament-id})))]
    (db/batch-create-judges data)))

(defn school-id-by-code [schools code]
  (->> schools (filter #(= (:code %) code)) first :_id))

(defn team-id-by-name [teams code school-id]
  (->> teams (filter #(and (= (:team-code %) code) (= (:school-id %) school-id))) first :_id))

(defn upload-teams [tournament-id teams-file]
  (let [schools (db/get-schools tournament-id)
        raw-data (read-csv-file teams-file)
        teams-data (map #(into {}
                               {:team-code     (first %)
                                :school-id     (school-id-by-code schools (second %))
                                :tournament-id tournament-id}) raw-data)]
    (db/batch-create-teams teams-data)
    (let [teams (db/get-teams tournament-id)
          speaker-data (mapcat
                         (fn [row]
                           (mapv #(into {}
                                        {:name          %
                                         :team-id       (team-id-by-name teams
                                                                         (first row)
                                                                         (school-id-by-code schools (second row)))
                                         :tournament-id tournament-id})
                                 (drop 2 row))) raw-data)]
      (db/batch-create-speakers speaker-data))))

(defn create-tournament [owner {:keys [name team-count speak-count rooms-file schools-file judges-file teams-file]}]
  (log/info "Creating tournament" name "for" owner)
  (let [owner-id (:_id (db/get-user owner))
        tournament-id (:_id (db/create-tournament owner-id name (Integer/parseInt team-count) (Integer/parseInt speak-count)))]
    (upload-rooms tournament-id rooms-file)
    (upload-schools tournament-id schools-file)
    (upload-judges tournament-id judges-file)
    (upload-teams tournament-id teams-file)
    "success"))

(defn get-tournaments [user]
  (let [user-id (:_id (db/get-user user))]
    (wrap-transit-resp
      (stringify-map
        (map #(assoc % :owner? (= user-id (:owner-id %))) (db/get-tournaments user-id))
        [:_id :owner-id :editors]))))

(defn get-tournament [tid]
  (wrap-transit-resp
    (stringify-reduce (db/get-tournament tid) [:_id :owner-id :editors])))

(defn get-rooms [tid]
  (wrap-transit-resp
    (stringify-map (db/get-rooms tid) [:_id :tournament-id])))

(defn get-teams [tid]
  (wrap-transit-resp
    (stringify-map (db/get-teams tid) [:_id :tournament-id :school-id])))

(defn get-speakers [tid]
  (wrap-transit-resp
    (stringify-map (db/get-speakers tid) [:_id :tournament-id :team-id])))

(defn get-teams-with-speakers [tid]
  )

(defn get-judges [tid]
  (wrap-transit-resp
    (stringify-map (db/get-judges tid) [:_id :tournament-id])))

(defn get-scratches [tid]
  (wrap-transit-resp
    (stringify-map (db/get-scratches tid) [:_id :tournament-id :team-id :judge-id])))

(defn get-schools [tid]
  (wrap-transit-resp
    (stringify-map (db/get-schools tid) [:_id :tournament-id])))

(defn get-rounds [tid]
  (wrap-transit-resp
    (stringify-map (db/get-rounds tid) [:_id :tournament-id])))

(defn delete-tournament [id]
  (db/delete-tournament id)
  "success")

(defn create-round [tid]
  (wrap-transit-resp
    (stringify-reduce (db/create-round tid) [:_id :tournament-id])))

(defn delete-round [rid]
  (db/delete-round rid)
  "success")

(defroutes tabber-routes
           (GET "/" [] (app-page))

           (GET "/ajax/tournaments" {:keys [session]} (get-tournaments (:identity session)))
           (POST "/ajax/tournaments" {:keys [session params]} (create-tournament (:identity session) params))
           (context "/ajax/tournaments/:tid" [tid]
             (GET "/" [] (get-tournament tid))
             (GET "/rooms" [] (get-rooms tid))
             (GET "/teams" [] (get-teams tid))
             (GET "/teams-speakers" [] (get-teams-with-speakers tid))
             (GET "/speakers" [] (get-speakers tid))
             (GET "/judges" [] (get-judges tid))
             (GET "/scratches" [] (get-scratches tid))
             (GET "/schools" [] (get-schools tid))
             (GET "/rounds" [] (get-rounds tid))
             (DELETE "/delete" [] (delete-tournament tid))
             (POST "/rounds/new" [] (create-round tid))
             (POST "/rounds/:rid/autopair" [rid] (prn tid rid))
             (DELETE "/rounds/:rid" [rid] (delete-round rid))))

