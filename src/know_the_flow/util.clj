(ns know-the-flow.util)

(defn epoch-time []
  (quot (System/currentTimeMillis) 1000))

(defn gallons-to-liters [gal]
  (* gal 3.7854118))
