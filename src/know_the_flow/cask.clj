(ns know-the-flow.cask)

;; Cask Msg record structure - These messages are modification events
;; to be applied to a cask.
;;
;; ts: timestamp
;; op: operation [:change :reset]
;; ml: milliliters
;; src: src of message [:meter :api]
(defrecord Msg [ts op ml src])

(defn create-cask
  "Return ref to a new cask.  Capacity in milliliters"
  [capacity]
  (ref {:capacity capacity
        :remaining capacity
        :txns []}))

(defn update-cask [cask change]
  (case (:op change)
    :change (dosync
             (alter cask assoc-in [:txns] (conj (:txns @cask) change))
             (alter cask assoc-in [:remaining] (+ (:remaining @cask) (:ml change))))
    :reset (dosync
            (alter cask assoc-in [:txns] [])
            (alter cask assoc-in [:remaining] (:capacity @cask))))  
  cask)

(defn remaining [cask]
  (:remaining @cask))

(defn capacity [cask]
  (:capacity @cask))

(defn consumed-since
  "How much of our cask has been consumed since epoch ts?"
  [cask since]
  (if (empty? (:txns @cask))
    0
    (->> (:txns @cask)
         (filter #(>= (:ts %) since))
         (map #(:ml %))
         (apply +)
         (-))))
