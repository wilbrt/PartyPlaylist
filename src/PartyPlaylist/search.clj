(ns PartyPlaylist.search
  (:require [clj-http.client :as client]))

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
        (.split t "h1")
        (second t)
        (.split t ">")
        (nth t 2)
        (.split t "<")
        (first t)
        (str t)))

(defn videoname [query source]
  (cond (= source 0) (videonametube (videoid query 0))
        (= source 1) (videonamespot query)
        (= source 2) (videonamecloud query)))
