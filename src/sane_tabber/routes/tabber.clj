(ns sane-tabber.routes.tabber
  (:require [compojure.core :refer [defroutes GET POST context]]
            [ring.util.http-response :refer [ok]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [sane-tabber.layout :as layout]
            [sane-tabber.db.core :as db]
            [sane-tabber.utils :refer [stringify-reduce stringify-map]]))

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
                               :rating        (second %)
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
  (timbre/info "Creating tournament" name "for" owner)
  (let [owner-id (:_id (db/get-user owner))
        tournament-id (:_id (db/create-tournament owner-id name team-count speak-count))]
    (upload-rooms tournament-id rooms-file)
    (upload-schools tournament-id schools-file)
    (upload-judges tournament-id judges-file)
    (upload-teams tournament-id teams-file)
    (pr-str 1)))

(defn get-tournaments [user]
  (let [user-id (:_id (db/get-user user))]
    (pr-str
      (stringify-map
        (map #(assoc % :owner? (= user-id (:owner-id %))) (db/get-tournaments user-id))
        [:_id :owner-id :editors]))))

(defn get-rooms [tid]
  (pr-str (stringify-map (db/get-rooms tid) [:_id :tournament-id])))

(defn get-teams [tid]
  (pr-str (stringify-map (db/get-teams tid) [:_id :tournament-id :school-id])))

(defn get-speakers [tid]
  (pr-str (stringify-map (db/get-speakers tid) [:_id :tournament-id :team-id])))

(defn get-teams-with-speakers [tid]
  )

(defn get-judges [tid]
  (pr-str (stringify-map (db/get-judges tid) [:_id :tournament-id])))

(defn get-scratches [tid]
  (pr-str (stringify-map (db/get-scratches tid) [:_id :tournament-id :team-id :judge-id])))

(defn delete-tournament [id]
  (db/delete-tournament id)
  (pr-str 1))

(defroutes tabber-routes
           (GET "/" [] (app-page))

           (GET "/ajax/tournaments" {:keys [session]} (get-tournaments (:identity session)))
           (POST "/ajax/tournaments" {:keys [session params]} (create-tournament (:identity session) params))
           (context "/ajax/tournaments/:tid" [tid]
             (GET "/rooms" [] (get-rooms tid))
             (GET "/teams" [] (get-teams tid))
             (GET "/teams-speakers" [] (get-teams-with-speakers tid))
             (GET "/speakers" [] (get-speakers tid))
             (GET "/judges" [] (get-judges tid))
             (GET "/scratches" [] (get-scratches tid))
             (POST "/delete" [] (delete-tournament tid))))

