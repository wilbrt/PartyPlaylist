(ns PartyPlaylist.web
  (:require [compojure.core :refer :all]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [clj-http.client :as client]
            [ring.util.request :as rur]
            [PartyPlaylist.urls :as urls]
            [PartyPlaylist.database :as database]
            [PartyPlaylist.search :as search])
        (:use [hiccup.core]))


(defroutes app
  (GET "/" [] urls/frontpage)
  (GET "/seuraava" [] database/seuraava)
  (GET "/videot" [] urls/videohaku)
  (GET "/soittolista" [] urls/soittolista)
  (POST "/luohuone" [] database/luohuone)
  (POST "/asd" [] database/create-url-handler)
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))
