(ns genqref-api.handler
  (:require [reitit.swagger-ui :as swagger-ui]
            [reitit.swagger :as swagger]
            [reitit.openapi :as openapi]
            [integrant.core :as ig]))

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

(defmethod ig/init-key ::get-resource [_ options]
  (fn [req]
    {:status 200 :body {:status "ok"}}))
    ;; [::response/ok {:everything "ok"}]))

(defmethod ig/init-key ::post-resource [_ options]
  (fn [req]
    {:status 200 :body {:status "ok"}}))
