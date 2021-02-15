(ns clojure_getting_started.urls
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.request :as rur]
            [clj-http.client :as client])
  (:use [hiccup.core]
        [hiccup.util]))

(def spec (or (System/getenv "DATABASE_URL")
            "postgresql://localhost:5432/pplaylist"))

(defn not-found [req]
  (html [:p "page not found"]))

(defn videoid [query]
        (let [teksti (str (client/get "https://www.youtube.com/results" {:query-params {"search_query" (str query)}}))
              t (str (nth (.split (second (.split teksti "videoId")) "\"") 2))]
  (subs t 0 (- (.length t) 1))))

(defn videoname [query]
  (let [teksti (str (client/get (str "https://www.youtube.com/watch?v=" query)))
        t (second (.split teksti "title>"))]
    (clojure.string/join "-" (reverse  (rest  (reverse (.split (subs t 0 (- (.length t) 2)) "-")))))))

(defn get-url-by-id
  "Gets the url from the database with the given id, or nil if no such
   url exists."
  [id]
  (let [query ["SELECT url FROM urls WHERE id = ?" id]
        result (jdbc/query spec query)]
        (:url (first result))))

(defn get-name-by-id
  "Gets the url from the database with the given id, or nil if no such
   url exists."
  [id]
  (let [query ["SELECT name FROM urls WHERE id = ?" id]
        result (jdbc/query spec query)]
        (:name (first result))))

(defn newsongnumber [n]
  (if-not (get-url-by-id n)
    n
    (newsongnumber (+ 1 n))))

(defn create-url!
  "Given a url as a string, creates a url row in the database and returns
   the created row."
  [url name]
  (let [id (newsongnumber 1)
        row {:name name :url url :id id}
        result (jdbc/insert! spec :urls row)]
    (first result)))

(defn get-url-handler
  [req]
  (let [id (get-in req [:params :id])
        url (get-url-by-id (Integer/parseInt id))]
    (if-not url
      (throw (not-found))
      {:status 200 :body url})))

(defn create-url-handler
  [req]
  (let [arr (str (first (get-in req [:params :url])))]
       (let [url (videoid arr)
              name (videoname url)
              row (create-url! url name)]
              {:status 200 :body row})))

(defn frontpage1
  [req]
(html [:a {:href "getinfo"} [:p "Hae id"]]
      [:a {:href "postinfo"} [:p "Lisää id"]]
      [:a {:href "videot"} [:p "videolista"]]))

(defn postinfo [req]
  (html [:form {:action "./asd" :method "post"}
         [:input {:type "text" :id "url" :name "url"}]
         [:input {:type "submit" :id "url" :name "url"}]]))


(defn getinfo [req]
  (let [query (get req :query-string)]
    (if-not query
      (html [:form
           [:input {:type "text" :id "lookup" :name "lookup"}]
           [:input {:type "submit"}]])
      {:status 200 :body (get-url-by-id (Integer/parseInt (str (last (.split query "=")))))})))


(defn update-table! []
      (jdbc/delete! spec :urls ["id = ?" 1])
      (jdbc/execute! spec ["update urls set id = id - 1 where id < ?" 30]))

(defn videohaku [req]
    (let [id (get-url-by-id 1)]
        (if id
            (html [:h
                   [:script
                    {:src "https://code.jquery.com/jquery-3.5.1.min.js" :integrity "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" :crossorigin "anonymous"}]
                   [:script
                    {:src "https://cdnjs.cloudflare.com/ajax/libs/jquery.form/4.3.0/jquery.form.min.js" :integrity "sha384-qlmct0AOBiA2VPZkMY3+2WqkHtIQ9lSdAsAn5RUJD/3vA5MKDgSGcdmIv4ycVxyn" :crossorigin "anonymous"}]
                   [:script "$(function() {
                        $('#addtolist').ajaxForm(function() {
                                  document.getElementById('lista').contentWindow.location.reload();
                              });
                          });"]]
                  [:div {:align "center"}
                  [:iframe {
                        :width "560"
                        :height "300"
                        :src (str "https://www.youtube.com/embed/" id "?autoplay=1")
                        :frameboarder "0"
                        :allow "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"}]]
                  [:div {:align "center"}
                   [:p (get-name-by-id 1)]]
                  [:div {:align "center"}
                        [:form {:id "addtolist" :action "./asd" :method "post"}
                        [:input {:type "text" :id "url" :name "url"}]
                        [:input {:type "submit" :id "url" :name "url"}]]
                  [:button {:type "submit" :value "Next" :onclick "window.location=\"./seuraava\";"} "Next"]]
                  [:div {:align "center"}
                  [:iframe {:id "lista"
                            :width "450"
                            :height "315"
                            :src "./soittolista"}]])
            (html [:h
                   [:script
                    {:src "https://code.jquery.com/jquery-3.5.1.min.js" :integrity "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" :crossorigin "anonymous"}]
                   [:script
                    {:src "https://cdnjs.cloudflare.com/ajax/libs/jquery.form/4.3.0/jquery.form.min.js" :integrity "sha384-qlmct0AOBiA2VPZkMY3+2WqkHtIQ9lSdAsAn5RUJD/3vA5MKDgSGcdmIv4ycVxyn" :crossorigin "anonymous"}]
                   [:script "$(function() {
                        $('#lomake').ajaxForm(function() {
                                  location.reload();
                              });
                          });"]]
                  [:p "Playlist empty"]
                  [:form {:action "./asd" :method "post" :id "lomake"}
                    [:input {:type "text" :id "url" :name "url"}]
                    [:input {:type "submit" :id "url" :name "url"}]]))))

(defn seuraava [req]
 (do
  (update-table!)
  (html [:script "window.location=\"./videot\";"])))

(defn soittolista [req]
  (html [:table (for [x (range 2 (newsongnumber 1))]
                  [:div {:align "center"}
                   [:tr
                   [:td (get-name-by-id  x)] "asdasdasd"]]
                  )]))
