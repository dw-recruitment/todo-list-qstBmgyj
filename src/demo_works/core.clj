(ns demo-works.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer :all]
            [compojure.core :refer :all]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
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
(defn insert-todo [todo] (sql/insert! @db-spec :todo todo))

;;
;; Business handlers
;;
(defn add-todo
  [input]
  (let [todo (get input "todo")
        record {:task todo :state ":todo"}]
    (when (and todo (not= "" todo))
      (insert-todo record))))

;;
;; Page rendering and routing
;;
(defn todo-list
  [todos]
  (html [:div
         [:h1 "TODO List"]
         [:ul 
          (for [todo todos]
            [:li (:task todo)])]
         (form-to [:post "/add"]
          (text-field {:placeholder "I need todo"} "todo")
          (submit-button "Add"))]))
  
(defn about-page
  []
  (html [:h1 "About"]
        [:p "This project is to demonstrate a basic web application to the phenomenal staff of Democracy Works!"]))

(defroutes app
  (GET "/" [] (todo-list (retrieve-todos)))
  (POST "/add" req (do
                     (add-todo (:params req))
                     (todo-list (retrieve-todos))))
  (GET "/about" [] (about-page)))

;;
;; Main execution
;;
(defn -main
  [database & args]
  (reset! db-spec (build-database database))
  (jetty/run-jetty (wrap-params app) {:port 3000}))
