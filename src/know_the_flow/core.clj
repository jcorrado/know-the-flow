(ns know-the-flow.core
  (:require [know-the-flow.serial :refer [init-port parse-ser]]
            [know-the-flow.cask :refer [create-cask update-cask]]
            [know-the-flow.util :refer [gallons-to-liters]]
            [know-the-flow.handler :as handler]
            [clojure.core.async :refer [go-loop chan >! <! >!! <!! put! take! alts!]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :as r]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre :refer [info]])
  (:gen-class))

(timbre/merge-config! {:timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss ZZ"}})
(timbre/set-level! :info)

(def http-port 3000)

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

(defn -main [tty]
  (info "Starting server")

  ;;
  ;; process cask update events
  ;;
  (let [ser-c (parse-ser (init-port tty))]
    (go-loop []
      (let [[msg _] (alts! [ser-c api-c])]
        (info "update event:" msg)
        (update-cask cask msg))
      (recur)))

  ;;
  ;; HTTP API
  ;;
  (-> api
      (wrap-json-response)
      (wrap-defaults api-defaults)
      (jetty/run-jetty {:port http-port})))
