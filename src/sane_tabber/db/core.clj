(ns sane-tabber.db.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.conversion :as conversion]
            [environ.core :refer [env]]
            [buddy.hashers :as hashers]
            [buddy.core.hash :refer [sha256]]
            [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.nonce :refer [random-nonce]])
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
       (ObjectId. ^String id)))))

(defn oid-conv [item]
  (if (coll? item) (mapv object-id item) (object-id item)))

(defn object-idify [m ks]
  (reduce #(update-in %1 [%2] oid-conv) m ks))

(defn get-by-tid [coll tid]
  (mc/find-maps @db coll {:tournament-id (object-id tid)}))

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
  (batch-insert "rooms" data))

(defn batch-create-judges [data]
  (batch-insert "rooms" data))

(defn batch-create-teams [data]
  (batch-insert "rooms" data))

(defn batch-create-speakers [data]
  (batch-insert "rooms" data))

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

(defn get-rooms [tournament-id]
  (mc/find-maps @db "rooms" {:tournament-id (object-id tournament-id)}))

(defn create-room [room]
  (insert-return @db "rooms" (object-idify room [:tournament-id])))

(defn update-room [room]
  (let [room (object-idify room [:_id :tournament-id])]
    (mc/update-by-id @db "rooms" (:_id room) {$set room})))

(defn create-team [team]
  (insert-return @db "teams" (:_id team) {$set team}))

(defn update-team [team]
  (let [team (object-idify team [:_id :tournament-id :school-id])]
    (mc/update-by-id @db "teams" (:_id team) {$set team})))

(defn create-speaker [speaker]
  (insert-return @db "speakers" (:_id speaker) {$set speaker}))

(defn update-speaker [speaker]
  (let [speaker (object-idify speaker [:_id :tournament-id :team-id])]
    (mc/update-by-id @db "speakers" (:_id speaker) {$set speaker})))
