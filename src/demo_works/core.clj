(ns demo-works.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [compojure.core :refer :all]
            [hiccup.core :refer :all]
            [clojure.java.jdbc :as sql])
  (:gen-class))

;;
;; Database functionality
;;
(def db-spec (atom nil))

(defn build-database
  [database-filename]
  {:classname "org.sqlite.jdbc"
   :subprotocol "sqlite"
   :subname database-filename})

(defn retrieve-todos [] (sql/query @db-spec "SELECT * FROM todo"))

;;
;; Page rendering and routing
;;
(defn todo-list
  [todos]
  (html [:div
         [:h1 "TODO List"]
         [:ul 
          (for [todo todos]
            [:li (:task todo)])]]))
  
(defn about-page
  []
  (html [:h1 "About"]
         [:p "This project is to demonstrate a basic web application to the phenomenal staff of Democracy Works!"]))

(defroutes app
  (GET "/" [] (todo-list (retrieve-todos)))
  (GET "/about" [] (about-page)))

;;
;; Main execution
;;
(defn -main
  [database & args]
  (reset! db-spec (build-database database))
  (jetty/run-jetty app {:port 3000}))
