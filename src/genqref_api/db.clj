(ns genqref-api.db
  (:require duct.database.sql
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql.builder :as builder]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.string :as str])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(def ^:dynamic *logger* nil)

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

;;; Helpers

(defn query-count [db resource]
  (sql/query db
             [(str "SELECT context, COUNT(*) FROM "
                   (name resource)
                   " GROUP BY context ORDER BY context")]
             jdbc/unqualified-snake-kebab-opts))

(defn upsert [table key-map opts]
  (let [[set-clause & set-args] (builder/by-keys key-map :set opts)
        conflict (str "ON CONSTRAINT " (name (get opts :conflict-contraint "upsert_unique")))
        suffix (str/join " " ["ON CONFLICT" conflict "DO UPDATE" set-clause])
        opts (assoc opts :suffix suffix)]
    (concat (builder/for-insert table key-map opts) set-args)))

#_(upsert :table {:a "b" :c "d"} jdbc/unqualified-snake-kebab-opts)

(defn upsert! [db table key-map opts]
  (jdbc/execute! db (upsert table key-map opts) opts))

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
  (count-markets [db])
  (create-market [db context validAt createdBy payload])
  (find-markets [db context]))

(extend-protocol Markets
  duct.database.sql.Boundary

  (count-markets [{db :spec}]
    (query-count db :markets))

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

(defprotocol Transactions
  (count-transactions [db])
  (create-transaction [db context createdBy payload])
  (find-transactions [db context]))

(extend-protocol Transactions
  duct.database.sql.Boundary

  (count-transactions [{db :spec}]
    (query-count db :transactions))

  (create-transaction [{db :spec} context createdBy payload]
    (sql/insert! db :transactions {:context context
                                   :created-by createdBy
                                   :payload payload}
                 jdbc/unqualified-snake-kebab-opts))

  (find-transactions [{db :spec} context]
    (sql/find-by-keys db :transactions {:context context}
                      jdbc/unqualified-snake-kebab-opts)))

(defprotocol Ships
  (count-ships [db])
  (create-ship [db context createdBy payload])
  (update-ship-projection [db context createdBy payload])
  (find-ships [db context]))

(extend-protocol Ships
  duct.database.sql.Boundary

  (count-ships [{db :spec}]
    (query-count db :ships))

  (create-ship [{db :spec} context createdBy payload]
    (sql/insert! db :ships {:context context
                            :created-by createdBy
                            :payload payload}
                 jdbc/unqualified-snake-kebab-opts))

  (update-ship-projection [{db :spec} context createdBy payload]
    (upsert! db :ships-projection {:context context
                                   :created-by createdBy
                                   :payload payload}
             jdbc/unqualified-snake-kebab-opts))

  (find-ships [{db :spec} context]
    (sql/find-by-keys db :ships {:context context}
                      jdbc/unqualified-snake-kebab-opts)))

(comment
  (def key-map {:a-b "c" :d "e"})
  (builder/as-? key-map jdbc/unqualified-snake-kebab-opts) ;; => "?, ?"
  (builder/as-cols key-map jdbc/unqualified-snake-kebab-opts) ;; => "a_b AS c, d AS e"
  (builder/as-keys key-map jdbc/unqualified-snake-kebab-opts) ;; => "a_b, d"
  (builder/by-keys key-map :set jdbc/unqualified-snake-kebab-opts) ;; => ["SET a_b = ?, d = ?" "c" "e"]
  (builder/by-keys key-map :where jdbc/unqualified-snake-kebab-opts) ;; => ["WHERE a_b = ? AND d = ?" "c" "e"]
  (builder/for-delete :table key-map jdbc/unqualified-snake-kebab-opts) ;; => ["DELETE FROM table WHERE a_b = ? AND d = ?" "c" "e"]
  (builder/for-insert :table key-map jdbc/unqualified-snake-kebab-opts) ;; => ["INSERT INTO table (a_b, d) VALUES (?, ?)" "c" "e"]
  (builder/for-update :table key-map {:de "e"} jdbc/unqualified-snake-kebab-opts)
  )

(defprotocol Shipyards
  (count-shipyards [db])
  (create-shipyard [db context createdBy payload])
  (find-shipyards [db context]))

(extend-protocol Shipyards
  duct.database.sql.Boundary

  (count-shipyards [{db :spec}]
    (query-count db :shipyards))

  (create-shipyard [{db :spec} context createdBy payload]
    (sql/insert! db :shipyards {:context context
                                :created-by createdBy
                                :payload payload}
                 jdbc/unqualified-snake-kebab-opts))

  (find-shipyards [{db :spec} context]
    (sql/find-by-keys db :shipyards {:context context}
                      jdbc/unqualified-snake-kebab-opts)))

(defprotocol Jumpgates
  (count-jumpgates [db])
  (create-jumpgate [db context createdBy payload])
  (find-jumpgates [db context]))

(extend-protocol Jumpgates
  duct.database.sql.Boundary

  (count-jumpgates [{db :spec}]
    (query-count db :jumpgates))

  (create-jumpgate [{db :spec} context createdBy payload]
    (sql/insert! db :jumpgates {:context context
                                :created-by createdBy
                                :payload payload}
                 jdbc/unqualified-snake-kebab-opts))

  (find-jumpgates [{db :spec} context]
    (sql/find-by-keys db :jumpgates {:context context}
                      jdbc/unqualified-snake-kebab-opts)))

(comment
  ;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.883/doc/all-the-options
  (def opts {:dbtype "postgresql"
             :dbname "genqref"
             :user "postgres"
             :port "5424"})
  (def db (duct.database.sql/->Boundary opts))

  (def callsign (create-callsign db 1 "ZAPP"))

  (->> callsign :token (find-callsign db))

  (->> callsign :token)

  (count-transactions db)

  (sql/query opts ["SELECT context, COUNT(*) FROM markets GROUP BY context;"] jdbc/unqualified-snake-kebab-opts)

  (upsert! opts :ships-projection {:context "2023"
                                   :created-by "me"
                                   :payload {:symbol "a"
                                             :value "2"}} jdbc/unqualified-snake-kebab-opts)
  )

"genqref-api.db"
