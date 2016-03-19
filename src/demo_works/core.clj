(ns demo-works.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [compojure.core :refer :all])
  (:gen-class))

(defn build-form
  []
  (str "<h1>UNDER CONSTRUCTION!</h1>"
       "<img src=\"http://media.galaxant.com/000/202/734/pic17564711gif\">"))

(defn about-page
  []
  "<h1>About</h1><p>This project is to demonstrate a basic web application to the phenomenal staff of Democracy Works!</p>")

(defroutes app
  (GET "/" [] (build-form))
  (GET "/about" [] (about-page)))

(defn -main
  [& args]
  (jetty/run-jetty app {:port 3000}))
