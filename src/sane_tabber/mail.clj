(ns sane-tabber.mail
  (:require [clojurewerkz.mailer.core :refer [*delivery-settings* deliver-email]]
            [environ.core :refer [env]]))

(defn settings! [m]
  (alter-var-root (var *delivery-settings*) (constantly m)))

(settings! {:user (:ses-user env) :pass (:ses-pass env)
            :host "email-smtp.us-west-2.amazonaws.com"
            :port 587})

(defn recovery-email [email link]
  (deliver-email {:from "SaneTabber <sanetabber@gmail.com>" :to [email] :subject "Forgot Your Password - SaneTabber"}
                 "templates/email/html_forgot.mustache" {:link link} :text/html
                 "templates/email/forgot.mustache" {:link link} :text/plain))
