(ns clojure_getting_started.urls
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.request :as rur]
            [clj-http.client :as client])
  (:use [hiccup.core]
        [hiccup.util]))

(def spec (or (System/getenv "DATABASE_URL")
            "postgresql://localhost:5432/pplaylist"))

(defn luohuone [req]
  (as-> req r
      (get-in r [:params :nimi])
      (first r)
      (str r)
      (jdbc/create-table-ddl r [[:id :int] [:url "varchar(255)"] [:name "varchar(255)"] [:source :int]]
                                  {:entities clojure.string/upper-case :conditional? true})
      #_(vec (str r))
      (jdbc/db-do-commands spec r))
  (html [:meta {:http-equiv "refresh" :content (str "0; URL=./videot?huone=" (str (first (get-in req [:params :nimi]))))}]
        [:p "Your room is being prepared."]))

(defn videoid [query source]
  (as-> query t
        (str t)
        (cond (= source 0) (as->  t a
                                  (client/get "https://www.youtube.com/results" {:query-params {"search_query" a}})
                                  (str a)
                                  (.split a "videoId"))
              (= source 1) (as-> t a
                                 (client/get (str "http://elegant-croissant.glitch.me/spotify?type=track&q=" a) {:accept :json})
                                 (str a)
                                 (.split a "spotify:track:")))
        (second t )
        (.split t "\"")
        (cond (= source 0) (->  t
                                  (nth 2))
              (= source 1) (->  t
                                  (first)))
        (str t)
        (subs t 0 (- (.length t) 1))))

(defn muuta [m c]
    (as-> m a
         (cond (= c 0)(-> a
                        (.split "name")
                        (last))
              (= c 1) (-> a
                        (.split "artists")
                        (last)
                        (.split "name")
                        (second)))
        (.split a "\"")
        (nth a 2)
        (str a)
        (subs a 0 (- (.length a) 1))))

(defn videonametube [query]
  (let [teksti (str (client/get (str "https://www.youtube.com/watch?v=" query)))
        t (second (.split teksti "title>"))]
    (clojure.string/join "-" (reverse  (rest  (reverse (.split (subs t 0 (- (.length t) 2)) "-")))))))

(defn videonamespot [q]
  (as-> q a
        (client/get (str "http://elegant-croissant.glitch.me/spotify?type=track&q=" a) {:accept :json})
        (str a)
        (.split a "spotify:track:")
        (first a)
        (str (muuta a 1) " - " (muuta a 0))))

(defn videoname [query source]
  (cond (= source 0) (videonametube (videoid query 0))
        (= source 1) (videonamespot query)))

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
  (let [r (str (first (get-in req [:params :url])))
        table (get-in req [:params :huone])
        source (Integer/parseInt (get-in req [:params :source]))]
    (str r table source)
    (-> (videoid r source)
              (create-url! (videoname r source) table source))))

(defn update-table! [table]
      (jdbc/delete! spec (keyword table) ["id = ?" 1])
      (jdbc/execute! spec [(str "update " table " set id = id - 1 where id < ?") 30]))

(defn player [table id source]
  #_(html [:iframe { :width "300" :height "380" :src "https://open.spotify.com/embed/track/5XcZRgJv3zMhTqCyESjQrF"
  :frameboarder "0" :allow "encrypted-media;" :allowtransparency "true"}])
  (if (and id source) (cond
                              (= source 0) (html  [:script "$(function() {
                                                $('#addtolist').ajaxForm(function() {
                                                  document.getElementById('lista').src = document.getElementById('lista').src;});});"]
                                   [:iframe { :width "560" :height "300" :src (str "https://www.youtube.com/embed/" id "?autoplay=1")
                                              :frameboarder "0" :allow "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"}])
                (= source 1) (html [:script "$(function() {
                                                $('#addtolist').ajaxForm(function() {
                                                  document.getElementById('lista').src = document.getElementById('lista').src;});});"]
                                   [:iframe { :width "300" :height "380" :src (str "https://open.spotify.com/embed/track/" id)
                                              :frameboarder "0" :allow "encrypted-media;" :allowtransparency "true"}]))
         (html [:script "$(function() {
              $('#addtolist').ajaxForm(function() {
                        location.reload();});});"]
               [:p "Playlist empty"])))

(defn videohaku [req]
  (let [table (get-in req [:params :huone])
        url (get-url-by-id 1 table)
        sorsa (get-source-by-id 1 table)]
      (html [:h [:script {:src "https://code.jquery.com/jquery-3.5.1.min.js" :integrity "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" :crossorigin "anonymous"}]
                [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/jquery.form/4.3.0/jquery.form.min.js" :integrity "sha384-qlmct0AOBiA2VPZkMY3+2WqkHtIQ9lSdAsAn5RUJD/3vA5MKDgSGcdmIv4ycVxyn" :crossorigin "anonymous"}]]
            [:div {:align "center"} (player table url sorsa)]
            [:div {:align "center"} [:p (get-name-by-id 1 table)]]
            [:div {:align "center"}
                [:form {:id "addtolist" :action "./asd" :onclick "setTimeout(() => {document.getElementById('lista').src = document.getElementById('lista').src;}, 2000);" :method "post"}
                      [:input {:type "text" :id "url" :name "url"}]
                      [:select {:id "source" :name "source" :form_id "addtolist"}
                          [:option {:value 0} "Youtube"]
                          [:option {:value 1} "Spotify"]]
                      [:input {:type "hidden" :id "huone" :name "huone" :value table}]
                      [:input {:type "submit" :id "url" :name "url"}]]
                [:button {:type "submit" :value "Next" :onclick (str "window.location=\"./seuraava?huone=" table "\";")} "Next"]
                           #_[:button {:type "submit" :value "ref" :onclick "document.getElementById('lista').src = document.getElementById('lista').src"} "Refresh Playlist"]]
            [:div {:align "center"}
                [:iframe {:id "lista" :width "450" :height "315" :src (str "./soittolista?huone=" table)}]])))


(defn frontpage [req]
  (html [:div {:align "center"}
          [:p "Type in a name to Create a new room or Join an existing one and pimp your party with a playlist!"]
          [:form {:id "nimi" :action "./luohuone"  :method "post"}
            [:input {:type "text" :id "nimi" :name "nimi"}]
            [:input {:type "submit" :id "nimi" :name "nimi" :value "Create/Join"}]]]))

(defn seuraava [req]
  (let [table (get-in req [:params :huone])]
    (do
      (update-table! table)
      (html [:script (str "window.location=\"./videot?huone=" table "\";" )]))))

(defn soittolista [req]
  (let [table (get-in req [:params :huone])]
      (html [:table (for [x (range 2 (newsongnumber 1 table))]
                      [:div {:align "center"}
                        [:tr
                        [:td (get-name-by-id x table)]]]
                  )])))
