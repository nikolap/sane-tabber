(ns sane-tabber.auth)

(defn authorized-access [req] (boolean (get-in req [:session :identity])))
(defn unauthorized-access [_] true)

(def rules
  [{:pattern #"^/login$"
    :handler unauthorized-access}
   {:pattern #"^/register$"
    :handler unauthorized-access}
   {:pattern #"^/forgot$"
    :handler unauthorized-access}
   {:pattern #"^/legal$"
    :handler unauthorized-access}
   {:pattern #"^/reset-password$"
    :handler unauthorized-access}
   {:pattern #"^/test$"
    :handler unauthorized-access}
   {:pattern #"^/assets/.*"
    :handler unauthorized-access}
   {:pattern  #"^/.*"
    :handler  authorized-access
    :redirect "/login"}])