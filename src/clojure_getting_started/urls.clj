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
  (if (= source 2)
               (as-> query a
                     (str a)
                     (client/get (str "https://soundcloud.com/search/sounds?q=" a) {:accept :json})
                     (str a)
                     (.split a "<h2><a href=\\\\\"")
                     (second a)
                     (.split a "\"")
                     (first a)
                     (str a)
                     (subs a 0 (- (.length a) 1))
                     (client/get (str "https://soundcloud.com" a))
                     (str a)
                     (.split a "soundcloud:tracks:")
                     (second a)
                     (.split a "\"")
                     (first a)
                     (str a)
                     (subs a 0 (- (.length a) 1)))

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
                     (cond (= source 0)      (->  t
                                                  (nth 2))
                           (= source 1)      (->  t
                                                  (first)))
                     (str t)
                     (subs t 0 (- (.length t) 1)))))

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

(defn videonamecloud [q]
  (as-> q t
        (client/get (str "https://soundcloud.com/search/sounds?q=" t) {:accept :json})
        (str t)
        (.split t "<h2><a href=\\\\\"")
        (second t)
        (.split t "\"")
        (first t)
        (str t)
        (subs t 0 (- (.length t) 1))
        (client/get (str "https://soundcloud.com" t))
        (str t)
        (.split t "Stream")
        (second t)
        (.split t "from")
        (first t)
        (.split t "by")
        (str (last t) "-" (first t))))

(defn videoname [query source]
  (cond (= source 0) (videonametube (videoid query 0))
        (= source 1) (videonamespot query)
        (= source 2) (videonamecloud query)))

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
    (-> (videoid r source)
              (create-url! (videoname r source) table source))))

(defn update-table! [table]
      (jdbc/delete! spec (keyword table) ["id = ?" 1])
      (jdbc/execute! spec [(str "update " table " set id = id - 1 where id < ?") 30]))

(defn player [table id source b]
  (if (and id source) (cond
                              (= source 0) (html  [:script "$(function() {
                                                $('#addtolist').ajaxForm(function() {
                                                  document.getElementById('lista').src = document.getElementById('lista').src;});});"]
                                   [:iframe { :width (if b "100%" "560") :height "300" :src (str "https://www.youtube.com/embed/" id "?autoplay=1")
                                              :frameboarder "0" :allow "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"}])
                              (= source 1) (html [:script "$(function() {
                                                $('#addtolist').ajaxForm(function() {
                                                  document.getElementById('lista').src = document.getElementById('lista').src;});});"]
                                   [:iframe { :width "300" :height "380" :src (str "https://open.spotify.com/embed/track/" id)
                                              :frameboarder "0" :allow "encrypted-media;" :allowtransparency "true"}])
                              (= source 2) (html [:script "$(function() {
                                                $('#addtolist').ajaxForm(function() {
                                                  document.getElementById('lista').src = document.getElementById('lista').src;});});"]
                                   [:iframe {:width "100%" :height "166" :scrolling "no" :frameborder "no"
                                             :allow "autoplay" :src (str "https://w.soundcloud.com/player/?url=https%3A//api.soundcloud.com/tracks/" id)}]))

                      (html [:script "$(function() {
                        $('#addtolist').ajaxForm(function() {
                          location.reload();});});"]
                          [:p "Playlist empty"])))

(defn findua [headers]
  (second (first (filter #(= "user-agent" (first %)) headers))))

(defn videohaku [req]
  (let [table (get-in req [:params :huone])
        url (get-url-by-id 1 table)
        sorsa (get-source-by-id 1 table)
        b (clojure.string/includes? (findua (get req :headers)) "Mobile")]
    (html [:h [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                [:link {:href "http://fonts.googleapis.com/css?family=Corben:bold" :rel "stylesheet" :type "text/css"}]
                [:link {:href "http://fonts.googleapis.com/css?family=Nobile" :rel "stylesheet" :type "text/css"}]
                [:style {:type "text/css"} (str (if b "#url { width: 50%; } #source { width: 25%; } #but { width: 20%; } #lista { width: 100%; }")
                              "h1, h2, h3, h4, h5, h6 { font-family: 'Corben', Georgia, Times, serif; font-size: 1.5em; }
                               p, div { font-family: 'Nobile', Helvetica, Arial, sans-serif; }
                               ")]
                [:script {:src "https://code.jquery.com/jquery-3.5.1.min.js" :integrity "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" :crossorigin "anonymous"}]
                [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/jquery.form/4.3.0/jquery.form.min.js" :integrity "sha384-qlmct0AOBiA2VPZkMY3+2WqkHtIQ9lSdAsAn5RUJD/3vA5MKDgSGcdmIv4ycVxyn" :crossorigin "anonymous"}]]
            [:div {:align "center"} [:h1 (str "Room Name: " (clojure.string/upper-case (str table)))]]
            [:div {:align "center"} (player table url sorsa b)]
            [:div {:align "center"} [:p (get-name-by-id 1 table)]]
            [:div {:align "center" :id "search"}
                [:form {:id "addtolist" :action "./asd" :onclick "setTimeout(() => {document.getElementById('lista').src = document.getElementById('lista').src;}, 2000);" :method "post"}
                      [:input {:type "text" :id "url" :name "url"}]
                      [:select {:id "source" :name "source" :form_id "addtolist" :style "margin-left: 2px; margin-right: 2px;"}
                          [:option {:value 0} "Youtube"]
                          [:option {:value 1} "Spotify"]
                          [:option {:value 2} "SoundCloud"]]
                      [:input {:type "hidden" :id "huone" :name "huone" :value table}]
                      [:input {:type "submit" :id "but" :name "but"}]]
                [:button {:type "submit" :value "Next" :onclick (str "window.location=\"./seuraava?huone=" table "\";")} "Next"]
                           #_[:button {:type "submit" :value "ref" :onclick "document.getElementById('lista').src = document.getElementById('lista').src"} "Refresh Playlist"]]
            [:div {:align "center" :style "margin-top: 10px;"}
                [:iframe {:id "lista" :height "315" :allowtransparency "true" :src (str "./soittolista?huone=" table)}]])))


(defn frontpage [req]
  (html   [:h [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
              [:link {:href "http://fonts.googleapis.com/css?family=Corben:bold" :rel "stylesheet" :type "text/css"}]
              [:link {:href "http://fonts.googleapis.com/css?family=Nobile" :rel "stylesheet" :type "text/css"}]
              [:style {:type "text/css"}
                "h1, h2, h3, h4, h5, h6 { font-family: 'Corben', Georgia, Times, serif; font-size: 1.5em; }
                 p, div { font-family: 'Nobile', Helvetica, Arial, sans-serif; }"]]
          [:div {:align "center"}
          [:h1 "Welcome to Party Playlist!"]
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
      (html [:h [:style {:type "text/css"} "body { background-color : transparent; }"]]
            [:table (for [x (range 2 (newsongnumber 1 table))]
                      [:div {:align "center"}
                        [:tr
                        [:td (get-name-by-id x table)]]]
                  )])))
