(ns genqref-api.handler
  (:require [reitit.swagger-ui :as swagger-ui]
            [reitit.swagger :as swagger]
            [reitit.openapi :as openapi]
            [integrant.core :as ig]
            [clojure.string :as str]
            [genqref-api.db :as db]
            [taoensso.timbre :as log]))

(defmethod ig/init-key ::swagger-ui [_ options]
  (swagger-ui/create-swagger-ui-handler))

(defmethod ig/init-key ::swagger [_ options]
  (swagger/create-swagger-handler))

(defmethod ig/init-key ::openapi [_ options]
  (openapi/create-openapi-handler))

(defmethod ig/init-key ::status [_ options]
  (fn [{[_] :ataraxy/result}]
    {:status 200 :body {:status "ok"}}))
;;[::response/ok {:status "ok"}]))

(def resources #{:markets :shipyards :jumpgates :ships :surveys :agents})

;; TODO: spec or malli markets

(defn valid-resource? [resource]
  (when-not (contains? resources (keyword resource))
    (throw (ex-info (str "Unknown resource '" resource "'."
                         " Supported resources: " (str/join ", " (sort (map name resources))))
                    {:resource resource
                     :supported (sort (map name resources))}))))

;; http :3000/api/2023/markets 'Authorization: Token \\xaebdb9fbf7db40fb83463aeb3d480c5729aac12998a74058620c295704f40862'
(defmethod ig/init-key ::get-resource [_ {:keys [logger db]}]
  (fn [{{:keys [context resource]} :path-params
        {:keys [waypoint system]} :query-params
        :as req}]
    (valid-resource? resource)
    (let [result (db/find-markets db context)]
      {:status 200 :body (map :payload result)})))
;; [::response/ok {:everything "ok"}]))

;; http ":3000/api/2023/markets?validAt=2023-03-09T12:42:12.123Z" a=b c=d
(defmethod ig/init-key ::post-resource [_ {:keys [logger db]}]
  (fn [{{:keys [context resource]} :path-params
        {:keys [validAt]} :params
        :keys [body-params]
        :as req}]
    (binding [db/*logger* logger]
      (let [record (db/create-market db context validAt (-> req :identity :symbol) body-params)]
        {:status 200 :body record}))))
