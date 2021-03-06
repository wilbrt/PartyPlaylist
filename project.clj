(defproject PartyPlaylist "1.0.0-SNAPSHOT"
  :description "Playlist combiner in Clojure"
  :url "http://pplaylist.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [lein-ring "0.8.8"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [environ "1.1.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.2.9"]
                 [hiccup "1.0.5"]
                 [clj-http "3.12.0"]
                 [cheshire "5.10.1"]
                 [proto-repl "0.3.1"]]

  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "PartyPlaylist.jar"
  :profiles {:production {:env {:production true}}})
