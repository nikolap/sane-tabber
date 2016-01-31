(ns sane-tabber.core
  (:require [sane-tabber.handler :refer [app init destroy]]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [config.core :refer [env]]
            [sane-tabber.db.core :refer [connect! disconnect!]])
  (:gen-class))

(defn parse-port [port]
  (when port
    (cond
      (string? port) (Integer/parseInt port)
      (number? port) port
      :else          (throw (Exception. (str "invalid port value: " port))))))

(defn http-port [port]
  ;;default production port is set in
  ;;env/prod/resources/config.edn
  (parse-port (or port (env :port))))

(defn stop-app []
  (repl/stop)
  (http/stop destroy)
  (disconnect!)
  (shutdown-agents))

(defn start-app
  "e.g. lein run 3000"
  [[port]]
  (let [port (http-port port)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))
    (when-let [repl-port (env :nrepl-port)]
      (repl/start {:port (parse-port repl-port)}))
    (connect!)
    (http/start {:handler app
                 :init    init
                 :port    port})))

(defn -main [& args]
  (start-app args))
