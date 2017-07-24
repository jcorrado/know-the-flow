(ns know-the-flow.handler
  (:require [know-the-flow.cask :as cask]
            [know-the-flow.util :refer [epoch-time]]
            [clojure.core.async :refer [put!]]
            [ring.util.response :as r]))

(defn- fmt-response [body]
  (-> body
      (r/response)
      (r/content-type "application/json")))

(defn remaining [cask new-cask-threshold]
  (let [capacity (cask/capacity cask)
        consumed (cask/consumed-since cask 0)]
    (if (< consumed  new-cask-threshold)
      (fmt-response {:capacity capacity :remaining 0})
      (fmt-response {:capacity capacity :remaining (cask/remaining cask)}))))

(defn consumed-since [cask since]
  (let [since (Integer/parseInt since)]
    (fmt-response {:ts since :consumed (cask/consumed-since cask since)})))

(defn update-cask [c vol]
  (let [vol (Integer/parseInt vol)]
    (if (= vol 0)
      (put! c (cask/->Msg (epoch-time) :reset 0 :api))
      (put! c (cask/->Msg (epoch-time) :change vol :api))))
  (r/response "OK"))
