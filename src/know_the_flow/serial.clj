(ns know-the-flow.serial
  (:require [know-the-flow.cask :refer [->Msg]]
            [know-the-flow.util :refer [epoch-time]]
            [clojure.core.async :refer [go-loop chan <! >! put!]]
            [clojure.string :as str]
            [serial.core :as serial]))

(def tty-speed 115200)

(def pulses-per-liter 5600)

(defn- pulse-to-ml
  "Convert pulses from flow meter to milliliters"
  [pulses]
  (Math/round (* 1000 (float (/ pulses pulses-per-liter)))))

(defn- parse-proto-block
  "Parse Arduino-defined serial protocol strings, returning a .cask.Msg record.
  Note: we munge ml to a negative, as we are decrementing our cask
  model.

  Example protocol messages:
  P;0;8;1241
  K;0;8"
  [msg]
  (let [f (str/split msg #";")
        op (f 0)
        ts (epoch-time)
        msg (->Msg ts :reset 0 :meter)]
    (if (= op "P")
      (let [ml (- (pulse-to-ml (Integer/parseInt (f 3))))]
        (-> msg
            (assoc :op :change)
            (assoc :ml ml)))
      msg)))

(def serial-msg-xf
  "Transducer: group bytes into raw messages, as defined by Arduino
  serial protocol, and produce Msg records."
  (comp
   (map char)
   (filter #(not= \return %))
   (partition-by #(= \newline %))
   (map #(apply str %))
   (filter #(not= "\n" %))
   (map parse-proto-block)))

(defn init-port
  "Setup callback for incoming bytes on serial port, returning a
  channel"
  ([tty]
   (init-port tty tty-speed))
  ([tty, speed]
   (let [c (chan 1 serial-msg-xf)
         port (serial/open tty :baud-rate speed)]
     (serial/listen! port #(put! c (.read  %)))
     c)))
