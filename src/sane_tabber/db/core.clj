(ns sane-tabber.db.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.conversion :as conversion]
            [environ.core :refer [env]]
            [buddy.hashers :as hashers]
            [buddy.core.hash :refer [sha256]]
            [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.nonce :refer [random-nonce]]
            [sane-tabber.utils :refer [max-inc]])
  (:import (org.bson.types ObjectId)))

(defonce db (atom nil))

(defn connect! []
  ;; Tries to get the Mongo URI from the environment variable
  (reset! db (-> (:database-url env) mg/connect-via-uri :db)))

(defn disconnect! []
  (when-let [conn @db]
    (mg/disconnect conn)
    (reset! db nil)))

;; UTILS

(defn from-dbo [dbo]
  (conversion/from-db-object dbo true))

(def insert-return (comp from-dbo mc/insert-and-return))

(defn object-id
  ([]
   (ObjectId.))
  ([id]
   (when id
     (if (instance? ObjectId id)
       id
       (when-not (clojure.string/blank? id)
         (ObjectId. ^String id))))))

(defn oid-conv [item]
  (if (coll? item) (mapv object-id item) (object-id item)))

(defn object-idify [m ks]
  (prn m ks)
  (reduce #(update-in %1 [%2] oid-conv) m ks))

(defn get-by-tid [coll tid & [args]]
  (mc/find-maps @db coll (merge {:tournament-id (object-id tid)} args)))

(defn batch-insert [coll data]
  (mc/insert-batch @db coll data))

;; SECURITY

(defn create-user [user]
  (insert-return @db "users" (assoc user :password (hashers/encrypt (:password user)))))

(defn update-user [id password]
  (mc/update-by-id @db "users" (object-id id) {$set {:password (hashers/encrypt password)}}))

(defn get-user [username]
  (mc/find-one-as-map @db "users" {:username username}))

(defn get-user-by-email [email]
  (mc/find-one-as-map @db "users" {:email email}))

(defn create-reset [{:keys [_id]}]
  (insert-return @db "resets" {:user-id _id
                               :token   (-> _id (str (bytes->hex (random-nonce 12))) sha256 bytes->hex)}))

(defn get-reset [token]
  (mc/find-one-as-map @db "resets" {:token token}))

(defn delete-reset [token]
  (mc/remove @db "resets" {:token token}))

;; TABBER

(defn create-tournament [owner name team-count speak-count]
  (insert-return @db "tournaments" {:owner-id    (object-id owner)
                                    :name        name
                                    :team-count  team-count
                                    :speak-count speak-count
                                    :editors     []}))

(defn delete-tournament [tournament-id]
  (let [tid (object-id tournament-id)]
    (mc/remove @db "rooms" {:tournament-id tid})
    (mc/remove @db "schools" {:tournament-id tid})
    (mc/remove @db "judges" {:tournament-id tid})
    (mc/remove @db "teams" {:tournament-id tid})
    (mc/remove @db "speakers" {:tournament-id tid})
    (mc/remove-by-id @db "tournaments" tid)))

(defn batch-create-rooms [data]
  (batch-insert "rooms" data))

(defn batch-create-schools [data]
  (batch-insert "schools" data))

(defn batch-create-judges [data]
  (batch-insert "judges" data))

(defn batch-create-teams [data]
  (batch-insert "teams" data))

(defn batch-create-speakers [data]
  (batch-insert "speakers" data))

(defn get-schools [tid]
  (get-by-tid "schools" tid))

(defn get-teams [tid]
  (get-by-tid "teams" tid))

(defn get-speakers [tid]
  (get-by-tid "speakers" tid))

(defn get-judges [tid]
  (get-by-tid "judges" tid))

(defn update-judge [judge]
  (let [judge (object-idify judge [:_id :tournament-id])]
    (mc/update-by-id @db "judges" (:_id judge) {$set judge})))

(defn create-judge [judge]
  (insert-return @db "judges" (object-idify judge [:tournament-id])))

(defn update-scratch [scratch]
  (let [scratch (object-idify scratch [:_id :tournament-id :team-id :judge-id])]
    (mc/update-by-id @db "scratches" (:_id scratch) {$set scratch})))

(defn create-scratch [scratch]
  (insert-return @db "scratches" (object-idify scratch [:tournament-id :team-id :judge-id])))

(defn delete-scratch [scratch]
  (mc/remove-by-id @db "scratches" (object-id (:_id scratch))))

(defn get-scratches [tid]
  (get-by-tid "scratches" tid))

(defn get-tournaments
  ([]
   (mc/find-maps @db "tournaments" {}))
  ([user-id]
   (mc/find-maps @db "tournaments" {$or [{:owner-id (object-id user-id)}
                                         {:editors {$in [(object-id user-id)]}}]})))

(defn get-tournament [tid]
  (mc/find-map-by-id @db "tournaments" (object-id tid)))

(defn get-rooms [tid]
  (mc/find-maps @db "rooms" {:tournament-id (object-id tid)}))

(defn create-room [room]
  (insert-return @db "rooms" (object-idify room [:tournament-id])))

(defn update-room [room]
  (let [room (object-idify room [:_id :tournament-id])]
    (mc/update-by-id @db "rooms" (:_id room) {$set room})))

(defn create-team [team]
  (insert-return @db "teams" (object-idify team [:tournament-id])))

(defn update-team [team]
  (let [team (object-idify team [:_id :tournament-id :school-id])]
    (mc/update-by-id @db "teams" (:_id team) {$set team})))

(defn create-speaker [speaker]
  (insert-return @db "speakers" (object-idify speaker [:tournament-id])))

(defn update-speaker [speaker]
  (let [speaker (object-idify speaker [:_id :tournament-id :team-id])]
    (mc/update-by-id @db "speakers" (:_id speaker) {$set speaker})))

(defn get-rounds [tid]
  (mc/find-maps @db "rounds" {:tournament-id (object-id tid)}))

(defn get-round [rid]
  (mc/find-map-by-id @db "rounds" (object-id rid)))

(defn create-round [tid]
  (let [round-num (->> tid
                       get-rounds
                       (map :round-number)
                       max-inc)]
    (insert-return @db "rounds" {:round-number  round-num
                                 :tournament-id (object-id tid)})))

(defn delete-round [rid]
  (mc/remove-by-id @db "rounds" (object-id rid)))

(defn get-all-round-rooms [tid]
  (mc/find-maps @db "round-rooms" {:tournament-id (object-id tid)}))

(defn get-all-scored-round-rooms [tid]
  (mc/find-maps @db "round-rooms" {:tournament-id (object-id tid)
                                   :ballot {$exists true}}))

(defn get-round-rooms [rid]
  (mc/find-maps @db "round-rooms" {:round-id (object-id rid)}))

(defn get-active-teams [tid]
  (get-by-tid "teams" tid {:signed-in? true}))

(defn get-active-rooms [tid]
  (get-by-tid "rooms" tid {:disabled? false}))

(defn get-active-judges [tid]
  (get-by-tid "judges" tid {:dropped? true}))

(defn batch-insert-round-rooms [tid rid round-rooms]
  (batch-insert "round-rooms" (map #(assoc % :tournament-id (object-id tid)
                                             :round-id (object-id rid)) round-rooms)))

(defn update-round [round]
  (mc/save @db "rounds" round))

(defn clear-round-room-data [rid]
  (mc/remove @db "round-rooms" {:round-id (object-id rid)}))

(defn create-round-room [round-room]
  (insert-return @db "round-rooms" (object-idify round-room [:tournament-id :round-id :room :judges])))

(defn update-round-room [round-room]
  (let [round-room (object-idify round-room [:_id :tournament-id :round-id :room :judges])]
    (mc/update-by-id @db "round-rooms" (:_id round-room) {$set round-room})))