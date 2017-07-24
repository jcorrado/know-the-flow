(ns know-the-flow.core
  (:require [know-the-flow.serial :refer [init-port parse-ser]]
            [know-the-flow.cask :refer [create-cask update-cask]]
            [know-the-flow.util :refer [gallons-to-liters write-txn]]
            [know-the-flow.handler :as handler]
            [clojure.core.async :refer [go-loop chan >! <! >!! <!! put! take! alts!]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :as r]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre :refer [info]]
            [taoensso.timbre.appenders.core :as appenders])
  (:gen-class))

(def daemon-log "know-the-flow.log")
(def txn-log "know-the-flow.txn")

(timbre/merge-config!
 {:timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss ZZ"}
  :appenders {:spit (appenders/spit-appender {:fname daemon-log})}
  :output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}})})

(timbre/set-level! :info)

;; our standard coldbrew keg is 5 gal.
(def cask (create-cask (int (* 1000 (gallons-to-liters 5)))))

;; cask update msgs from our API
(def api-c (chan 1))

;;
;; API routes
;;
(defroutes api
  (GET "/cask"        []      (handler/remaining cask))
  (GET "/cask/:since" [since] (handler/consumed-since cask since))
  (PUT "/cask/:vol"   [vol]   (handler/update-cask api-c vol))
  (route/not-found "Page not found"))

(defn -main [tty http-port]
  (info "Starting know-the-flow server")

  ;;
  ;; process cask update events
  ;;
  (let [ser-c (parse-ser (init-port tty))]
    (go-loop []
      (let [[msg _] (alts! [ser-c api-c])]
        (info "update event:" msg)
        (update-cask cask msg)
        (write-txn txn-log msg))
      (recur)))

  ;;
  ;; HTTP API
  ;;
  (-> api
      (wrap-json-response)
      (wrap-defaults api-defaults)
      (jetty/run-jetty {:port (Integer/parseInt http-port)})))
