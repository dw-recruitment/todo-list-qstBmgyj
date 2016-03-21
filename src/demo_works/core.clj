(ns demo-works.core
  (:require [demo-works.db :as db]
            [demo-works.view :as view]
            [demo-works.domain :refer :all]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer :all]
            [ring.middleware.session :refer :all]
            [ring.util.response :as response]
            [compojure.core :refer :all])
  (:gen-class))

;;
;; Actions
;;
(defn login-user
  "Setup the session with the user ID"
  [user-id]
  (-> user-id
      db/retrieve-user
      view/home-page
      response/response
      (assoc :session {:user-id user-id})))

(defn show-login 
  [] 
  (view/login-page (db/retrieve-users)))

(defn get-user-id [session] (:user-id session))

;;
;; Routing
;;
(defroutes app
  (GET "/" [] (show-login))
  (GET "/login/:id" [id] (login-user id))
  (GET "/logout" [:as {session :session}] (-> (response/response (show-login))
                                              (assoc :session nil)))
  (GET "/home" [:as {session :session}] (-> (get-user-id session) db/retrieve-user view/home-page))
  (POST "/add/:list-id" [list-id todo :as {session :session}] (add-todo (get-user-id session) list-id todo)
                                                              (response/redirect "/home"))
  (POST "/edit/:id" [id submit :as {session :session}] (let [user-id (get-user-id session)]
                                                         (if (= "Delete" submit)
                                                           (delete-todo (get-user-id session) id)
                                                           (toggle-todo (get-user-id session) id))
                                                         (response/redirect "/home")))
  (GET "/about" [] (view/about-page)))

;;
;; Main execution
;;
(defn -main
  [& args]
  (if (= 1 (count args))
    (do 
      (db/init-db (first args))
      (jetty/run-jetty (-> app
                           wrap-params
                           (wrap-session {:cookie-attrs {:max-age 3600}}))
                       {:port 3000}))
    (println "Usage: lein run <database file>")))
