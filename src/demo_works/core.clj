(ns demo-works.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer :all]
            [ring.middleware.session :refer :all]
            [ring.util.response :as response]
            [compojure.core :refer :all]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.element :refer [link-to]]
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

(defn db-retrieve-users [] (sql/query @db-spec "SELECT * FROM user"))
(defn db-retrieve-user [user-id] (first (sql/query @db-spec ["SELECT * FROM user WHERE id = ?" user-id])))
(defn db-retrieve-lists [user-id] (sql/query @db-spec ["SELECT l.*, a.role 
                                                       FROM list l 
                                                       JOIN list_acl a ON l.id = a.list_id 
                                                       WHERE a.user_id = ?" user-id]))
(defn db-retrieve-list [user-id list-id] (sql/query @db-spec ["SELECT l.* 
                                                               FROM list l
                                                               JOIN list_acl a ON l.id = a.list_id
                                                               WHERE l.id = ? AND a.user_id = ?" list-id user-id]))
(defn db-retrieve-todos [user-id list-id] (sql/query @db-spec ["SELECT t.* 
                                                               FROM todo t
                                                               JOIN list_acl a ON t.list_id = a.list_id
                                                               WHERE t.list_id = ? AND a.user_id = ?" list-id user-id]))
(defn db-retrieve-todo [user-id id] (first (sql/query @db-spec ["SELECT * 
                                                                 FROM todo t
                                                                 JOIN list_acl a ON t.list_id = a.list_id
                                                                 WHERE t.id = ? AND a.user_id = ?" id user-id])))
(defn db-insert-todo [todo] (sql/insert! @db-spec :todo todo))
(defn db-update-todo [todo] (sql/update! @db-spec :todo {:state (:state todo)} ["id = ?" (:id todo)]))
(defn db-delete-todo [id] (sql/delete! @db-spec :todo ["id = ?" id]))

;;
;; Business handlers
;;
(defn add-todo
  [user-id list-id todo]
  (when (and todo 
             (not= "" todo)
             (db-retrieve-list user-id list-id))
  (let [record {:list_id list-id :task todo :state ":todo"}]
    (db-insert-todo record))))

(defn toggle-state [state] (if (= ":todo" state) ":done" ":todo"))

(defn toggle-todo
  [user-id id]
  (some-> (db-retrieve-todo user-id id)
          (update :state toggle-state)
          db-update-todo))

(defn delete-todo
  [user-id id]
  (when (db-retrieve-todo user-id id)
    (db-delete-todo id)))

(defn get-lists-by-user
  [user-id]
  (->> user-id
       db-retrieve-lists
       (map (fn [l] 
              (assoc l :todos (db-retrieve-todos user-id (:id l)))))))

;;
;; Page rendering
;;
(defn format-todo
  [todo]
  (let [id (:id todo)
        done (= ":done" (:state todo))]
    (form-to [:post (str "/edit/" id)]
              [:div 
               (if done 
                 [:del (:task todo)]
                 (:task todo))
               (submit-button {:name "submit"} (if done
                                                 "Undo"
                                                 "Complete"))
               (submit-button {:name "submit"} "Delete")])))

(defn format-list
  [list]
  (let [list-id (:id list)]
    [:div
     [:h2 (:name list)]
     [:ul 
      (for [todo (:todos list)]
        [:li
         (format-todo todo)])
      (form-to [:post (str "/add/" list-id)]
               (text-field {:placeholder "I need todo"} "todo")
               (submit-button "Add"))]]))

(defn format-lists
  [lists]
  [:ul
   (for [list lists]
     [:li
      (format-list list)])])

(defn home-page
  [user-id]
  (let [lists (get-lists-by-user user-id)
        user (db-retrieve-user user-id)]
    (html [:div
           [:h1 (str "Hello " (:name user))]
           (link-to "/logout" "Logout")
           (format-lists lists)])))

(defn login-page
  []
  (let [users (db-retrieve-users)]
    (html [:div
           [:h1 "User Login"]
           [:ul
            (for [user users]
              [:li
               (link-to (str "/login/" (:id user)) (:name user))])]])))
  
(defn about-page
  []
  (html [:h1 "About"]
        [:p "This project is to demonstrate a basic web application to the phenomenal staff of Democracy Works!"]))

;;
;; Routing and helpers
;;
(defn login-user
  "Setup the session with the user ID"
  [user-id]
  (-> (response/response (home-page user-id))
      (assoc :session {:user-id user-id})))

(defn get-user-id [session] (:user-id session))

(defroutes app
  (GET "/" [] (login-page))
  (GET "/login/:id" [id] (login-user id))
  (GET "/logout" [:as {session :session}] (-> (response/response (login-page))
                                              (assoc :session nil)))
  (GET "/home" [:as {session :session}] (-> (get-user-id session)
                                            home-page))
  (POST "/add/:list-id" [list-id todo :as {session :session}] (add-todo (get-user-id session) list-id todo)
                                                              (response/redirect "/home"))
  (POST "/edit/:id" [id submit :as {session :session}] (let [user-id (get-user-id session)]
                                                         (if (= "Delete" submit)
                                                           (delete-todo (get-user-id session) id)
                                                           (toggle-todo (get-user-id session) id))
                                                         (response/redirect "/home")))
  (GET "/about" [] (about-page)))

;;
;; Main execution
;;
(defn -main
  [database & args]
  (reset! db-spec (build-database database))
  (jetty/run-jetty (-> app
                       wrap-params
                       (wrap-session {:cookie-attrs {:max-age 3600}}))
                   {:port 3000}))
