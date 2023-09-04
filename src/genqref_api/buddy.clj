(ns genqref-api.buddy
  (:require [integrant.core :as ig]
            [genqref-api.db :as db]
            [buddy.auth :as auth]))

;; yes, this munges authentication and authorization together, which
;; is good for now, I guess. (But if you're a student of mine this
;; will cost you points!)
(defmethod ig/init-key ::token-auth [_ {:keys [db]}]
  (fn [request token]
    (or (first (db/find-callsign db token))
        (auth/throw-unauthorized))))
