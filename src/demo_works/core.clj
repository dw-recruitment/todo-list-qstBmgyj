(ns demo-works.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer :all]
            [ring.util.response :as response]
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
(defn retrieve-todo [id] (first (sql/query @db-spec ["SELECT * FROM todo WHERE id = ?" id])))
(defn insert-todo [todo] (sql/insert! @db-spec :todo todo))
(defn update-todo [todo] (sql/update! @db-spec :todo todo ["id = ?" (:id todo)]))

;;
;; Business handlers
;;
(defn add-todo
  [input]
  (let [todo (get input "todo")
        record {:task todo :state ":todo"}]
    (when (and todo (not= "" todo))
      (insert-todo record))))

(defn toggle-state [state] (if (= ":todo" state) ":done" ":todo"))

(defn toggle-todo
  [id]
  (-> id
      retrieve-todo
      (update :state toggle-state)
      update-todo))

;;
;; Page rendering and routing
;;
(defn todo-list
  [todos]
  (html [:div
         [:h1 "TODO List"]
         [:ul 
          (for [todo todos]
            (let [id (:id todo)
                  done (= ":done" (:state todo))]
              (form-to [:post (str "/toggle/" id)]
                       [:li 
                        [:div 
                         (if done 
                           [:del (:task todo)]
                           (:task todo))
                         (submit-button (if done
                                          "Undo"
                                          "Complete"))]])))]
         (form-to [:post "/add"]
          (text-field {:placeholder "I need todo"} "todo")
          (submit-button "Add"))]))
  
(defn about-page
  []
  (html [:h1 "About"]
        [:p "This project is to demonstrate a basic web application to the phenomenal staff of Democracy Works!"]))

(defroutes app
  (GET "/" [] (todo-list (retrieve-todos)))
  (POST "/add" req (add-todo (:params req))
                   (response/redirect "/"))
  (POST "/toggle/:id" [id] (toggle-todo id)
                           (response/redirect "/"))
  (GET "/about" [] (about-page)))

;;
;; Main execution
;;
(defn -main
  [database & args]
  (reset! db-spec (build-database database))
  (jetty/run-jetty (wrap-params app) {:port 3000}))
