(ns PartyPlaylist.urls
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.request :as rur]
            [clj-http.client :as client]
            [PartyPlaylist.database :as database]
            [PartyPlaylist.search :as search])
  (:use [hiccup.core]
        [hiccup.util]))

(def spec (or (System/getenv "DATABASE_URL")
                    "postgresql://localhost:5432/pplaylist"))

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
        url (database/get-url-by-id 1 table)
        sorsa (database/get-source-by-id 1 table)
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
            [:div {:align "center"} [:p (database/get-name-by-id 1 table)]]
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
          [:p "Type in a name (only alphanumerics allowed!) to Create a new room or Join an existing one and pimp your party with a playlist!"]
          [:form {:id "nimi" :action "./luohuone"  :method "post"}
            [:input {:type "text" :id "nimi" :name "nimi"}]
            [:input {:type "submit" :id "nimi" :name "nimi" :value "Create/Join"}]]]))


(defn soittolista [req]
  (let [table (get-in req [:params :huone])]
      (html [:h [:style {:type "text/css"} "body { background-color : transparent; }"]]
            [:table (for [x (range 2 (database/newsongnumber 1 table))]
                      [:div {:align "center"}
                        [:tr
                        [:td (database/get-name-by-id x table)]]]
                  )])))
