(ns genqref-api.db
  (:require duct.database.sql
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]
            [cheshire.core :as json]
            [taoensso.timbre :as log])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(def ^:dynamic *logger*)

;;; next.jdbc and postgres' json support

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (json/generate-string x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (json/parse-string value true) {:pgtype type}))
      value)))

;; if a SQL parameter is a Clojure hash map or vector, it'll be
;; transformed to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))
  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure
;; data while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))

;;; Records

;; (defprotocol Users
;;   (create-user [db email password])
;;   (find-user [db email password]))
;;
;; (extend-protocol Users
;;   duct.database.sql.Boundary
;;   (create-user [{db :spec} email password]
;;     (sql/insert! db :users {:email email
;;                              :password password}))
;;   (find-user [{db :spec} email password]
;;     (sql/find-by-keys db :users {:email email
;;                                  :password password})))

(defprotocol Callsigns
  (create-callsign [db user-id symbol])
  (find-callsign [db token]))

(extend-protocol Callsigns
  duct.database.sql.Boundary

  (create-callsign [{db :spec} user-id symbol]
    (sql/insert! db :callsigns {:user_id user-id
                                :symbol symbol}
                 jdbc/unqualified-snake-kebab-opts))

  (find-callsign [{db :spec} token]
    (sql/find-by-keys db :callsigns {:token token}
                      jdbc/unqualified-snake-kebab-opts)))

(comment
  ;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.883/doc/all-the-options
  (def opts {:dbtype "postgresql"
             :dbname "genqref"
             :user "postgres"
             :port "5424"})
  (def db-spec (duct.database.sql/->Boundary opts))

  (def callsign (create-callsign db-spec 1 "ZAPP"))

  (->> callsign :token (find-callsign db-spec))

  (->> callsign :token)
  )

;; (defprotocol Agents
;;   (create-agent [db token payload])
;;   (find-agent [db token]))
;;
;; (extend-protocol Agents
;;   duct.database.sql.Boundary
;;   (create-agent [{db :spec} payload]
;;     (sql/insert! db :agents {:context context
;;                               :validAt validAt
;;                               :createdBy createdBy
;;                               :payload payload}))
;;   (find-agent [{db :spec} callsign]
;;     (sql/find-by-keys db :agents {:context context
;;                                   :callsign callsign})))

(defprotocol Markets
  (create-market [db context validAt createdBy payload])
  (find-markets [db context]))

(extend-protocol Markets
  duct.database.sql.Boundary

  (create-market [{db :spec} context validAt createdBy payload]
    (let [db (jdbc/with-logging db #(log/info *logger* %1 %2))]
      (sql/insert! db :markets (merge
                                (when validAt {:valid-at validAt})
                                {:context context
                                 :created-by createdBy
                                 :payload payload})
                   jdbc/unqualified-snake-kebab-opts)))

  (find-markets [{db :spec} context]
    (sql/find-by-keys db :markets {:context context}
                      jdbc/unqualified-snake-kebab-opts)))

"genqref-api.db"
