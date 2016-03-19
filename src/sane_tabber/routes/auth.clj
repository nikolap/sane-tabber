(ns sane-tabber.routes.auth
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [buddy.hashers :as hashers]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :refer [hex->bytes bytes->hex]]
            [ring.util.response :as ring-resp]
            [bouncer.core :as bouncer]
            [bouncer.validators :as validators :refer [defvalidator]]
            [sane-tabber.layout :as layout]
            [sane-tabber.db.core :as db]
            [sane-tabber.mail :as mail]))

(defn login-page [& [errors]]
  (layout/render "login.html" {:errors errors}))

(defn register-page [& [errors]]
  (layout/render "register.html" {:errors errors}))

(defn forgot-page [& [errors]]
  (layout/render "forgot.html" {:errors errors}))

(defn reset-page [token & [errors]]
  (layout/render "reset.html" {:errors errors :token token}))

(defn logout! [session]
  (log/info (:identity session) "has logged out")
  (assoc (ring-resp/redirect "/login") :session (assoc session :identity nil)))

(defn authenticate [session username]
  (assoc (ring-resp/redirect "/") :session (assoc session :identity username)))

(defn login! [username password session]
  (log/info username "attempting to login")
  (if-let [dbuser (db/get-user username)]
    (if (hashers/check password (:password dbuser))
      (authenticate session username)
      (login-page ["Incorrect username/password combination"]))
    (login-page ["Please enter a valid username in order to login"])))

(defvalidator password-matches-validator
              {:default-message-format "Please ensure your passwords match"}
              [password verify-password]
              (= password verify-password))

(defvalidator username-not-exist-validator
              {:default-message-format "This username already exists"}
              [username]
              (nil? (db/get-user username)))

(defvalidator email-not-exist-validator
              {:default-message-format "This email is in use"}
              [email]
              (nil? (db/get-user-by-email email)))

(defn validate-registration [data]
  (bouncer/validate data
                    :username [[validators/required :message "Please enter a username"] username-not-exist-validator]
                    :email [[validators/matches #".+@.+" :message "Please enter a valid email address"] email-not-exist-validator]
                    :password [[validators/required :message "Please enter a password"]
                               [password-matches-validator (:verify-password data)]]))

(defn register! [data session]
  (log/info (:username data) "attempting to register")
  (let [[errors _] (validate-registration data)]
    (if errors
      (register-page (-> errors first val))
      (authenticate session (:username (db/create-user (dissoc data :__anti-forgery-token :verify-password)))))))

(defn generate-forgot-link [host user]
  (str "http://" host "/reset-password?token=" (:token (db/create-reset user))))

(defn forgot! [email host]
  (log/info email "forgot password")
  (if-let [user (db/get-user-by-email email)]
    (do
      (mail/recovery-email email (generate-forgot-link host user))
      (ring-resp/redirect "/login"))
    (forgot-page ["Please enter a registered email address"])))

(defn reset-password! [token password]
  (log/info "Attempting to reset password for token" token)
  (if (not-empty password)
    (if-let [reset-item (db/get-reset token)]
      (do
        (db/update-user (:user-id reset-item) password)
        (db/delete-reset token)
        (ring-resp/redirect "/login"))
      (reset-page token ["Invalid password reset link provided"]))
    (reset-page token ["Please enter a password"])))

(defroutes auth-routes
           (GET "/login" [] (login-page))
           (GET "/register" [] (register-page))
           (GET "/forgot" [] (forgot-page))
           (GET "/reset-password" [token] (reset-page token))
           (GET "/account" [] (println "awwww shite"))
           (GET "/logout" [session] (logout! session))
           (POST "/login" [username password session] (login! username password session))
           (POST "/register" {:keys [params session]} (register! params session))
           (POST "/forgot" {:keys [params headers]} (forgot! (:email params) (get headers "host")))
           (POST "/reset-password" [token password] (reset-password! token password)))