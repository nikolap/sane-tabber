(ns sane-tabber.routes.tabber
  (:require [compojure.core :refer [defroutes routes GET POST DELETE context]]
            [ring.util.http-response :refer [ok]]
            [clojure.data.csv :as csv]
            [clojure.tools.logging :as log]
            [ring.util.response :as ring-resp]
            [buddy.auth.accessrules :refer [restrict error]]
            [sane-tabber.layout :as layout]
            [sane-tabber.db.core :as db]
            [sane-tabber.utils :refer [stringify-reduce stringify-map wrap-transit-resp]]
            [sane-tabber.pairings :refer [pair-round pair-judges-only pair-teams-to-existing-rooms clean-string]]
            [sane-tabber.reporting :refer [teams-tab speakers-tab round-pairings team-position-stats export-teams export-judges]]
            [sane-tabber.statistics :refer [pairing-stats]]))

(defn app-page []
  (layout/render "home.html"))

(defn read-csv-file [file]
  (-> file :tempfile slurp csv/read-csv rest))

(defn upload-rooms [tournament-id rooms-file]
  (let [data (->> rooms-file read-csv-file (map #(into {}
                                                       {:name          (first %)
                                                        :tournament-id tournament-id
                                                        :disabled?     false
                                                        :accessible?   false})))]
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
                               :tournament-id tournament-id
                               :signed-in?    false
                               :accesible?    false})))]
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
                                :tournament-id tournament-id
                                :signed-in?    false
                                :accessible?   false}) raw-data)]
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

(defn get-users [_]
  (wrap-transit-resp
    (stringify-map (db/get-users) [:_id])))

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

(defn get-all-round-rooms [tid]
  (wrap-transit-resp
    (stringify-map (db/get-all-round-rooms tid) [:_id :tournament-id :round-id :room :judges])))

(defn get-round-rooms [rid]
  (wrap-transit-resp
    (stringify-map (db/get-round-rooms rid) [:_id :tournament-id :round-id :room :judges])))

(defn get-pairing-stats [tid]
  (let [teams (db/get-teams tid)
        round-data (db/get-all-scored-round-rooms tid)]
    (wrap-transit-resp
      (stringify-map (pairing-stats round-data teams) [:id]))))

(defn delete-tournament [id]
  (db/delete-tournament id)
  "success")

(defn create-round [tid]
  (wrap-transit-resp
    (stringify-reduce (db/create-round tid) [:_id :tournament-id])))

(defn autopair-round [tid rid]
  (log/info "Autopairing round for tournament" tid "and round" rid)
  (db/clear-round-room-data rid)
  (let [teams (db/get-active-teams tid)
        judges (map (partial clean-string :rating) (db/get-active-judges tid))
        rooms (db/get-active-rooms tid)
        scratches (db/get-scratches tid)
        round-rooms (db/get-all-scored-round-rooms tid)
        round (db/get-round rid)]
    (db/batch-insert-round-rooms tid rid (pair-round teams judges rooms scratches round-rooms))
    (db/update-round (assoc round :status "paired"))
    "success"))

(defn autopair-judges-only [tid rid]
  (log/info "Autopairing judges only for tournament" tid "and round" rid)
  (db/clear-round-room-data rid)
  (let [teams (db/get-active-teams tid)
        judges (map (partial clean-string :rating) (db/get-active-judges tid))
        rooms (db/get-active-rooms tid)
        tournament (db/get-tournament tid)
        round (db/get-round rid)]
    (db/batch-insert-round-rooms tid rid (pair-judges-only judges rooms (count teams) (:team-count tournament)))
    (db/update-round (assoc round :status "partial"))
    "success"))

(defn autopair-teams-to-existing [tid rid]
  (log/info "Autopairing teams to existing rounds for tournament" tid "and round" rid)
  (let [teams (db/get-active-teams tid)
        scratches (db/get-scratches tid)
        rooms (db/get-active-rooms tid)
        prev-round-data (db/get-all-scored-round-rooms tid)
        round-rooms (db/get-round-rooms rid)
        round (db/get-round rid)]
    (db/reinsert-round-rooms tid rid (pair-teams-to-existing-rooms teams rooms scratches prev-round-data round-rooms))
    (db/update-round (assoc round :status "paired"))
    "success"))

(defn delete-round [rid]
  (db/delete-round rid)
  "success")

(defn add-editor [{:keys [tid id]}]
  (wrap-transit-resp
    (stringify-reduce (db/from-dbo (db/add-editor tid id)) [:_id :owner-id :editors])))

(defn remove-editor [{:keys [tid id]}]
  (wrap-transit-resp
    (stringify-reduce (db/from-dbo (db/remove-editor tid id)) [:_id :owner-id :editors])))

(defn as-csv [data csv-name]
  (let [resp (ring-resp/response data)
        disp (str "attachment; filename=\"" csv-name "\"")]
    (-> resp
        (ring-resp/header "content-disposition" disp)
        (ring-resp/content-type "text/csv;charset=utf-8"))))

(defn file-name-prep [{:keys [name]} suffix]
  (-> name
      (str suffix)
      clojure.string/lower-case
      (clojure.string/replace " " "-")))

(defn team-tab-report [tid]
  (log/info "Generating team tab report for tournament" tid)
  (let [teams (db/get-teams tid)
        schools (db/get-schools tid)
        round-data (db/get-all-scored-round-rooms tid)
        rounds (db/get-rounds tid)
        tournament (db/get-tournament tid)]
    (as-csv (teams-tab teams schools round-data rounds)
            (file-name-prep tournament "-team-tab.csv"))))

(defn speaker-tab-report [tid]
  (log/info "Generating team tab report for tournament" tid)
  (let [speakers (db/get-speakers tid)
        teams (db/get-teams tid)
        schools (db/get-schools tid)
        round-data (db/get-all-scored-round-rooms tid)
        rounds (db/get-rounds tid)
        tournament (db/get-tournament tid)]
    (as-csv (speakers-tab speakers teams schools round-data rounds)
            (file-name-prep tournament "-speaker-tab.csv"))))

(defn team-stats-report [tid]
  (log/info "Generating team position stats report for tournament" tid)
  (let [tournament (db/get-tournament tid)
        teams (db/get-teams tid)
        schools (db/get-schools tid)
        round-data (db/get-all-scored-round-rooms tid)]
    (as-csv (team-position-stats teams schools round-data tournament)
      (file-name-prep tournament "-position-stats.csv"))))

(defn team-report [tid]
  (log/info "Generating team report for tournament" tid)
  (let [tournament (db/get-tournament tid)
        teams (filter :signed-in? (db/get-teams tid))
        schools (db/get-schools tid)
        speakers (db/get-speakers tid)]
    (as-csv
      (export-teams teams speakers schools)
      (file-name-prep tournament "-teams.csv"))))

(defn judge-report [tid]
  (log/info "Generating judge report for tournament" tid)
  (let [tournament (db/get-tournament tid)
        judges (db/get-judges tid)]
    (as-csv
      (export-judges judges)
      (file-name-prep tournament "-judges.csv"))))

(defn round-pairings-report [tid rid]
  (log/info "Generating round pairings for round" rid)
  (let [round (db/get-round rid)
        rooms (db/get-rooms tid)
        teams (db/get-teams tid)
        schools (db/get-schools tid)
        judges (db/get-judges tid)
        round-data (db/get-round-rooms rid)
        tournament (db/get-tournament tid)
        speakers (db/get-speakers tid)]
    (as-csv (round-pairings rooms teams schools judges round-data tournament speakers)
            (file-name-prep tournament (str "-round-" (:round-number round) "-pairings.csv")))))

(defn restriction [allowed?]
  (if allowed?
    true
    (error "You do not have permissions to make this action")))

(defn editor? [{:keys [session params]}]
  (let [user-id (-> session :identity db/get-user :_id)
        tournament (-> params :tid db/get-tournament)
        editors (conj (:editors tournament) (:owner-id tournament))]
    (restriction (contains? (set editors) user-id))))

(defn owner? [{:keys [session params]}]
  (let [user-id (-> session :identity db/get-user :_id)
        owner (-> params :tid db/get-tournament :owner-id)]
    (restriction (= owner user-id))))

(defn unauth-handler [request _]
  (layout/error-page
    {:status 403
     :title  (str "Access to " (:uri request) " is not authorized")}))

(defroutes tabber-routes
           (GET "/" [] (app-page))

           (GET "/ajax/tournaments" {:keys [session]} (get-tournaments (:identity session)))
           (POST "/ajax/tournaments" {:keys [session params]} (create-tournament (:identity session) params))
           (context "/ajax/tournaments/:tid" [tid]
             (restrict
               (routes
                 (GET "/" [] (get-tournament tid))
                 (GET "/rooms" [] (get-rooms tid))
                 (GET "/teams" [] (get-teams tid))
                 (GET "/speakers" [] (get-speakers tid))
                 (GET "/judges" [] (get-judges tid))
                 (GET "/scratches" [] (get-scratches tid))
                 (GET "/schools" [] (get-schools tid))
                 (GET "/rounds" [] (get-rounds tid))
                 (GET "/round-rooms" [] (get-all-round-rooms tid))
                 (GET "/:rid/round-rooms" [rid] (get-round-rooms rid))
                 (GET "/:rid/pairing-stats" [] (get-pairing-stats tid))
                 (GET "/users" [] (restrict
                                    get-users
                                    {:handler  owner?
                                     :on-error unauth-handler}))
                 (DELETE "/delete" [] (restrict
                                        (fn [_] (delete-tournament tid))
                                        {:handler  owner?
                                         :on-error unauth-handler}))
                 (POST "/editors/add" {:keys [params]} (restrict
                                           (fn [_] (add-editor params))
                                           {:handler  owner?
                                            :on-error unauth-handler}))
                 (POST "/editors/remove" {:keys [params]} (restrict
                                              (fn [_] (remove-editor params))
                                              {:handler  owner?
                                               :on-error unauth-handler}))
                 (POST "/rounds/new" [] (create-round tid))
                 (POST "/rounds/:rid/status" {:keys [params]} (prn "asdf"))
                 (POST "/rounds/:rid/autopair" [rid] (autopair-round tid rid))
                 (POST "/rounds/:rid/autopair-judges-first" [rid] (autopair-judges-only tid rid))
                 (POST "/rounds/:rid/autopair-teams-existing" [rid] (autopair-teams-to-existing tid rid))
                 (DELETE "/rounds/:rid" [rid] (delete-round rid)))
               {:handler  editor?
                :on-error unauth-handler}))

           (context "/tournaments/:tid/reports" [tid]
             (restrict
               (routes
                 (GET "/team-tab" [] (team-tab-report tid))
                 (GET "/speaker-tab" [] (speaker-tab-report tid))
                 (GET "/team-stats" [] (team-stats-report tid))
                 (GET "/teams" [] (team-report tid))
                 (GET "/judges" [] (judge-report tid))
                 (GET "/rounds/:rid/round-pairings" [rid] (round-pairings-report tid rid))
                 (GET "/rounds/:rid/round-ballots" [rid] (prn "asdf")))
               {:handler  editor?
                :on-error unauth-handler})))

