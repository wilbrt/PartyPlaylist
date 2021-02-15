(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [clj-http.client :as client]
            [ring.util.request :as rur]
            [clojure_getting_started.urls :as urls])
        (:use [hiccup.core]))


(defroutes app
  (GET "/" [] urls/frontpage1)
  (GET "/seuraava" [] urls/seuraava)
  (GET "/getinfo" [] urls/getinfo)
  (GET "/postinfo" [] urls/postinfo)
  (GET "/videot" [] urls/videohaku)
  (GET "/soittolista" [] urls/soittolista)
  (POST "/urls" [] urls/create-url-handler)
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
