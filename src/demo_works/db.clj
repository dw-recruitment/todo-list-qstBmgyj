(ns demo-works.db
  (:require [clojure.java.jdbc :as sql]))

(def db-spec (atom nil))

(defn build-database
  [database-filename]
  {:classname "org.sqlite.jdbc"
   :subprotocol "sqlite"
   :subname database-filename})

(defn init-db
  [database-filename]
  (reset! db-spec (build-database database-filename)))

(defn retrieve-users [] (sql/query @db-spec "SELECT * FROM user"))
(defn retrieve-user [user-id] (first (sql/query @db-spec ["SELECT * FROM user WHERE id = ?" user-id])))
(defn retrieve-lists [user-id] (sql/query @db-spec ["SELECT l.*, a.role 
                                                    FROM list l 
                                                    JOIN list_acl a ON l.id = a.list_id 
                                                    WHERE a.user_id = ?" user-id]))
(defn retrieve-list [user-id list-id] (sql/query @db-spec ["SELECT l.* 
                                                            FROM list l
                                                            JOIN list_acl a ON l.id = a.list_id
                                                            WHERE l.id = ? AND a.user_id = ?" list-id user-id]))
(defn retrieve-todos [user-id list-id] (sql/query @db-spec ["SELECT t.* 
                                                            FROM todo t
                                                            JOIN list_acl a ON t.list_id = a.list_id
                                                            WHERE t.list_id = ? AND a.user_id = ?" list-id user-id]))
(defn retrieve-todo [user-id id] (first (sql/query @db-spec ["SELECT * 
                                                              FROM todo t
                                                              JOIN list_acl a ON t.list_id = a.list_id
                                                              WHERE t.id = ? AND a.user_id = ?" id user-id])))
(defn insert-todo [todo] (sql/insert! @db-spec :todo todo))
(defn update-todo [todo] (sql/update! @db-spec :todo {:state (:state todo)} ["id = ?" (:id todo)]))
(defn delete-todo [id] (sql/delete! @db-spec :todo ["id = ?" id]))

