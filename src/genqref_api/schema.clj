(ns genqref-api.schema
  (:gen-class)
  (:require [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]
            [malli.transform :as mt]
            [cheshire.core :as json]))

#_(require '[malli.generator :as mg])

;;; Retrieve enums

(def url "https://stoplight.io/api/v1/projects/spacetraders/spacetraders/nodes/reference/SpaceTraders.json?fromExportButton=true&snapshotType=http_service&deref=optimizedBundle")

(def spec (json/parse-string (slurp url) true))

(defn enum [key]
  (case key
    :MarketTradeGoodSupply
    (-> spec :components :schemas :MarketTradeGood :properties :supply :enum)
    :MarketTransactionType
    (-> spec :components :schemas :MarketTransaction :properties :type :enum)
    :WaypointTraitSymbol
    (-> spec :components :schemas :WaypointTrait :properties :symbol :enum)
    :ShipCrewRotation
    (-> spec :components :schemas :ShipCrew :properties :rotation :enum)
    ;; else
    (-> spec :components :schemas key :enum)))

#_(enum :WaypointType)
#_(enum :SystemType)
#_(enum :WaypointTraitSymbol)

#_(-> spec :components :schemas keys)
#_(-> spec :components :schemas :ShipRole :enum)

(defn menum
  "m(alli)enum"
  [key]
  (vec (concat [:enum] (enum key))))

#_(def systems (json/parse-string (slurp "https://api.spacetraders.io/v2/systems.json") true))

(def SystemPattern #"^X1-[A-Z]{1,2}\d{1,2}$")

(def WaypointPattern #"^X1-[A-Z]{1,2}\d{1,2}-\d{5}[A-Z]$")

(def TimestampPattern #"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d.\d\d\dZ$")

(def ContextPattern #"^\w+$")

;;; Markets & Transactions

(def Commodity
  "An entry in imports, exports or exchange"
  [:map
   [:symbol (menum :TradeSymbol)]
   [:name {:optional true} string?]
   [:description {:optional true} string?]])

#_(mg/generate Commodity)

(def TradeGood
  [:map
   [:symbol (menum :TradeSymbol)]
   [:tradeVolume pos-int?]
   [:supply (menum :MarketTradeGoodSupply)]
   [:purchasePrice pos-int?]
   [:sellPrice pos-int?]])

#_(mg/generate TradeGood)

(def Transaction
  [:map
   [:waypointSymbol WaypointPattern]
   [:shipSymbol string?]
   [:tradeSymbol (menum :TradeSymbol)]
   [:type (menum :MarketTransactionType)]
   [:units pos-int?]
   [:pricePerUnit pos-int?]
   [:totalPrice pos-int?]
   [:timestamp TimestampPattern]])

#_(mg/generate Transaction)

(def Transactions
  [:sequential Transaction])

(def Market
  [:map
   [:symbol WaypointPattern]
   [:imports [:sequential Commodity]]
   [:exports [:sequential Commodity]]
   [:exchange [:sequential Commodity]]
   [:tradeGoods [:sequential TradeGood]]
   [:transactions {:optional true} Transactions]])

(def Markets
  [:sequential Market])

;;; Ships

(def Requirements
  [:map-of keyword? pos-int?])

(def Module
  [:map
   [:symbol string?]
   [:name {:optional true} string?]
   [:description {:optional true} string?]
   [:capacity {:optional true} pos-int?]
   [:requirements {:optional true} Requirements]])

(def Mount
  [:map
   [:symbol string?]
   [:name {:optional true} string?]
   [:description {:optional true} string?]
   [:strength {:optional true} pos-int?]
   [:requirements {:optional true} Requirements]])

;; TODO: refactor condition into UsableComponent

(def Reactor
  [:map
   [:symbol string?]
   [:name {:optional true} string?]
   [:description {:optional true} string?]
   [:condition pos-int?] ;; 0-100
   [:powerOutput {:optional true} pos-int?]
   [:requirements {:optional true} Requirements]])

(def Frame
  [:map
   [:symbol string?]
   [:name {:optional true} string?]
   [:description {:optional true} string?]
   [:moduleSlots {:optional true} pos-int?]
   [:mountingPoints {:optional true} pos-int?]
   [:fuelCapacity {:optional true} pos-int?]
   [:condition pos-int?]
   [:requirements {:optional true} Requirements]])

(def Engine
  [:map
   [:symbol string?]
   [:name {:optional true} string?]
   [:description {:optional true} string?]
   [:condition pos-int?]
   [:speed {:optional true} pos-int?]
   [:requirements {:optional true} Requirements]])

(def Ship
  [:map
   [:frame Frame]
   [:reactor Reactor]
   [:engine Engine]
   [:mounts [:sequential Mount]]
   [:modules [:sequential Module]]])

(def Cargo
  [:map
   [:capacity pos-int?]
   [:units pos-int?]
   [:inventory
    [:sequential
     [:map
      [:symbol string?]
      [:name {:optional true} string?]
      [:description {:optional true} string?]
      [:units pos-int?]]]]])

(def Crew
  [:map
   [:current pos-int?]
   [:capacity pos-int?]
   [:required pos-int?]
   [:rotation (menum :ShipCrewRotation)]
   [:morale pos-int?]
   [:wages pos-int?]])

(def Cooldown
  [:map
   [:shipSymbol {:optional true} string?]
   [:totalSeconds pos-int?]
   [:remainingSeconds {:optional true} pos-int?]
   [:expiration TimestampPattern]])

(def Fuel
  [:map
   [:current pos-int?]
   [:capacity pos-int?]
   [:consumed
    [:map
     [:amount pos-int?]
     [:timestamp TimestampPattern]]]])

(def Registration
  [:map
   [:name {:optional true} string?]
   [:factionSymbol string?] ;; enum
   [:role (menum :ShipRole)]])

(def Location
  [:map
   [:symbol WaypointPattern]
   [:type (menum :WaypointType)]
   [:systemSymbol SystemPattern]
   [:x int?]
   [:y int?]])

(def Nav
  [:map
   [:systemSymbol SystemPattern]
   [:waypointSymbol WaypointPattern]
   [:route
    [:map
     [:departure Location]
     [:destination Location]
     [:arrival TimestampPattern]
     [:departureTime TimestampPattern]]]
   [:status (menum :ShipNavStatus)]
   [:flightMode (menum :ShipNavFlightMode)]])

(def Usable
  "Aspects that make a ship usable."
  [:map
   [:symbol string?]
   [:crew Crew]
   [:cargo Cargo]
   [:cooldown Cooldown]
   [:fuel Fuel]
   [:registration Registration]
   [:nav Nav]])

(def UsableShip
  (mu/merge Usable Ship))

(def UsableShips
  [:sequential UsableShip])

(def Purchasable
  "Aspects that make a ship purchasable."
  [:map
   [:type (menum :ShipType)]
   [:name {:optional true} string?]
   [:description {:optional true} string?]
   [:purchasePrice pos-int?]])

(def PurchasableShip
  (mu/merge Purchasable Ship))

(def Shipyard
  [:map
   [:symbol WaypointPattern]
   [:shipTypes {:optional true}
    [:sequential
     [:map
      [:type (menum :ShipType)]]]]
   [:transactions {:optional true} Transactions]
   [:ships
    [:sequential PurchasableShip]]])

(def Shipyards
  [:sequential Shipyard])

(comment
  (def shipyard {:symbol "X1-AB12-12345A"
                 :ships [{:type "SHIP_PROBE"
                          :purchasePrice 1
                          :frame {:symbol ""
                                  :condition 100}
                          :reactor {:symbol ""
                                    :condition 100}
                          :engine {:symbol ""
                                   :condition 100}
                          :modules []
                          :mounts []}]})
  (me/humanize (m/explain Shipyard shipyard mt/json-transformer))
  )

"genqref-api.schema"
