(ns genqref-api.buddy
  (:require [integrant.core :as ig]))

;; this will eventually be replaced with a db query
(def tokens
   {:2f904e245c1f5 :admin
   :45c1f5e3f05d0 :foouser
   nil :hello})

(defmethod ig/init-key ::token-auth [_ options]
  (fn [request token]
    (let [token (keyword token)]
      (get tokens token nil))))
