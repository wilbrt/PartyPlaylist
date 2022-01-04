(ns PartyPlaylist.database
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.request :as rur]
            [clj-http.client :as client]
            [PartyPlaylist.search :as search])
  (:use [hiccup.core]
        [hiccup.util]))

(def spec (or (System/getenv "DATABASE_URL")
            "postgresql://localhost:5432/pplaylist"))

(defn luohuone [req]
  (if (re-find #"^[a-z\-]+$" (str (first (get-in req [:params :nimi]))))
      (as-> req r
        (get-in r [:params :nimi])
        (first r)
        (str r)
        (jdbc/create-table-ddl r [[:id :int] [:url "varchar"] [:name "varchar"] [:source :int]]
                                  {:entities clojure.string/upper-case :conditional? true})
        (jdbc/db-do-commands spec r)
        (html [:meta {:http-equiv "refresh" :content (str "0; URL=./videot?huone=" (str (first (get-in req [:params :nimi]))))}]
              [:p "Your room is being prepared."]))
        (html [:meta {:http-equiv "refresh" :content "2; URL=./frontpage"}]
              [:p "You are not allowed to use non-alphanumerics in the room name."])))


(defn get-url-by-id [id table]
            (let [query [(str "SELECT url FROM " table " WHERE id = ?") id]
                  result (jdbc/query spec query)]
                          (:url (first result))))

(defn get-source-by-id [id table]
            (let [query [(str "SELECT source FROM " table " WHERE id = ?") id]
                  result (jdbc/query spec query)]
                          (:source (first result))))


(defn get-name-by-id [id table]
            (let [query [(str "SELECT name FROM " table " WHERE id = ?") id]
                  result (jdbc/query spec query)]
                          (:name (first result))))

(defn newsongnumber [n table]
  (if-not (get-url-by-id n table)
    n
    (newsongnumber (+ 1 n) table)))

(defn create-url!
  "Given a url as a string, creates a url row in the database and returns
   the created row."
  [url name table source]
  (->> {:name name :url url :id (newsongnumber 1 table) :source source}
       (jdbc/insert! spec (keyword table))
       (first)))


(defn create-url-handler [req]
  (let [r (get-in req [:params :url])
        table (get-in req [:params :huone])
        source (Integer/parseInt (get-in req [:params :source]))]
    (-> (search/videoid r source)
              (create-url! (search/videoname r source) table source))))

(defn update-table! [table]
      (jdbc/delete! spec (keyword table) ["id = ?" 1])
      (jdbc/execute! spec [(str "update " table " set id = id - 1 where id < ?") 30]))

(defn seuraava [req]
  (let [table (get-in req [:params :huone])]
    (do
      (update-table! table)
      (html [:script (str "window.location=\"./videot?huone=" table "\";" )]))))
