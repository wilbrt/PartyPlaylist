(ns PartyPlaylist.search
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]))

(defn stripper [s]
  (subs s 0 (- (.length s) 1)))

(defn spotscrape [q]
  (as-> q t
       (:body (client/get (str "http://elegant-croissant.glitch.me/spotify?type=track&q=" t) {:accept :json}))
       (parse-string t true)
       (get-in t [:tracks :items])
       (first (keep #(if (= (:type %) "track") %) t))
        ))


(defn cloudpreprocess [s]
  (as-> s t
        (str (client/get (str "https://soundcloud.com/search/sounds?q=" t) {:accept :json}))
        (second (.split t "<h2><a href=\\\\\""))
        (first (.split t "\""))
        (stripper t)
        (str (client/get (str "https://soundcloud.com" t)))))

(defn videoid [query source]
  (cond (= source 2)
               (as-> query a
                     (cloudpreprocess a)
                     (second (.split a "soundcloud:tracks:"))
                     (first (.split a "\""))
                     (stripper a))
         (= source 1)
                (:id (spotscrape query))
         (= source 0)
                (as-> query t
                      (client/get "https://www.youtube.com/results" {:query-params {"search_query" t}})
                      (second (.split (str t) "videoId"))
                      (first (.split t "thumbnail"))
                      (nth (.split t "\"") 2)
                      (stripper t))))

(defn videonametube [query]
  (let [teksti (str (client/get (str "https://www.youtube.com/watch?v=" query)))
        t (second (.split teksti "title>"))]
    (clojure.string/join "-" (reverse  (rest  (reverse (.split (subs t 0 (- (.length t) 2)) "-")))))))

(defn videonamespot [q]
  (let [a (spotscrape q)]
        (str (:name (last (:artists a))) " - " (:name a))
       ))

(defn videonamecloud [q]
  (as-> q t
        (cloudpreprocess t)
        (second (.split t "h1"))
        (nth (.split t ">") 2)
        (first (.split t "<"))))

(defn videoname [query source]
  (cond (= source 0) (videonametube (videoid query 0))
        (= source 1) (videonamespot query)
        (= source 2) (videonamecloud query)))
