(ns genqref-api.handler
  (:require [reitit.swagger-ui :as swagger-ui]
            [reitit.swagger :as swagger]
            [reitit.openapi :as openapi]
            [integrant.core :as ig]
            [clojure.string :as str]
            [genqref-api.db :as db]
            [taoensso.timbre :as log]
            [malli.core :as m]
            [malli.error :as me]
            ;;[genqref-api.schema]
            ))

(defmethod ig/init-key ::swagger-ui [_ options]
  (swagger-ui/create-swagger-ui-handler))

(defmethod ig/init-key ::swagger [_ options]
  (swagger/create-swagger-handler))

(defmethod ig/init-key ::openapi [_ options]
  (openapi/create-openapi-handler))

(defmethod ig/init-key ::status [_ options]
  (fn [{[_] :ataraxy/result}]
    {:status 200 :body {:status "ok"}}))

(defmethod ig/init-key ::metrics [_ {:keys [db]}]
  (fn [req]
    (let [markets (db/count-markets db)
          transactions (db/count-transactions db)
          ships (db/count-ships db)]
      {:status 200 :body {:markets markets
                          :transactions transactions
                          :ships ships}})))

(def resources #{:markets :transactions :ships :shipyards :jumpgates})
;;(def resources #{:markets :shipyards :jumpgates :ships :surveys :agents :transactions})
;; -> derived table: agent_stats (credits ship_count update_frequency credit_trajectory)
;; -> derived table: trade_goods (symbol system_symbol waypoint_symbol purchase_price sell_price purchase_price_mean purchase_price_median ...)

(def resource-schema
  {;;:markets schema/Markets
   ;;:transactions schema/Transactions
   })

(defn validate-resource!
  ([resource]
   (when-not (contains? resources (keyword resource))
     (throw (ex-info (str "Unknown resource '" resource "'."
                          " Supported resources: " (str/join ", " (sort (map name resources))))
                     {:resource resource
                      :supported (sort (map name resources))}))))
  ([resource payload]
   (validate-resource! resource)
   (when-not (m/validate (resource-schema (keyword resource)) payload)
     (->> payload
          (m/explain (resource-schema (keyword resource)))
          me/humanize
          (hash-map :error)
          (ex-info (str "Invalid payload"))
          throw))))

(defmethod ig/init-key ::get-ships [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        {:keys [waypoint system]} :query-params
        :as req}]
    (let [result (db/find-ships db context)]
      {:status 200 :body (map :payload result)})))

(defmethod ig/init-key ::get-markets [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        {:keys [waypoint system]} :query-params
        :as req}]
    (let [result (db/find-markets db context)]
      {:status 200 :body (map :payload result)})))

(defmethod ig/init-key ::get-offers [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        {:keys [waypoint system]} :query-params
        :as req}]
    (let [result (db/find-markets db context)]
      {:status 200 :body (map :payload result)})))

(defmethod ig/init-key ::get-demands [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        {:keys [waypoint system]} :query-params
        :as req}]
    (let [result (db/find-markets db context)]
      {:status 200 :body (map :payload result)})))

;; TODO: this would look much nicer with specter
(defn lint-market [market]
  (-> market
      (dissoc :transactions)
      (update :imports (partial map #(dissoc % :name :description)))
      (update :exports (partial map #(dissoc % :name :description)))
      (update :exchange (partial map #(dissoc % :name :description)))))

(defn lint-ship [ship]
  (-> ship
      (update :cooldown dissoc
              :shipSymbol
              :remainingSeconds)
      (update :engine dissoc
              :name
              :description
              :speed
              :requirements)
      (update :frame dissoc
              :name
              :description
              :moduleSlots
              :mountingPoints
              :fuelCapacity
              :requirements)
      (update :reactor dissoc
              :name
              :description
              :powerOutput
              :requirements)
      (update :mounts (partial map #(dissoc %
                                            :name
                                            :description
                                            :strength
                                            :requirements)))
      (update :modules (partial map #(dissoc %
                                             :name
                                             :description
                                             :capacity
                                             :requirements)))))

(defmethod ig/init-key ::post-markets [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        {:keys [validAt]} :params
        :keys [body-params] :as req}]
    (future
      (let [reporter (-> req :identity :symbol)
            transactions (->> body-params (map :transactions) flatten (remove nil?))
            markets (map lint-market body-params)]
        (doseq [market markets]
          (db/create-market db context validAt reporter market))
        (doseq [transaction transactions]
          (db/create-transaction db context reporter transaction))))
    {:status 201}))

(defmethod ig/init-key ::post-transactions [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        :keys [body-params] :as req}]
    (future
      (let [reporter (-> req :identity :symbol)]
        (doseq [transaction body-params]
          (db/create-transaction db context reporter transaction))))
    {:status 201}))

(defmethod ig/init-key ::post-ships [_ {:keys [logger db]}]
  (fn [{{:keys [context]} :path-params
        {:keys [validAt]} :params
        :keys [body-params] :as req}]
    (future
      (let [reporter (-> req :identity :symbol)]
        (doseq [ship (map lint-ship body-params)]
          (db/create-ship db context reporter ship)
          (db/update-ship-projection db context reporter ship))))
    {:status 201}))
