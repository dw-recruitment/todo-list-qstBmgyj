(ns demo-works.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [ring.util.response :refer :all])
  (:gen-class))

(defn build-form
  []
  (str "<!DOCTYPE html><html><body>"
       "<h1>UNDER CONSTRUCTION!</h1>"
       "<img src=\"http://media.galaxant.com/000/202/734/pic17564711gif\">"
       "</body></html>"))

(defn handler
  "Handle incoming HTTP requests."
  [request]
  (response (build-form)))

(defn -main
  [& args]
  (jetty/run-jetty handler {:port 3000}))
