(ns demo-works.domain
  (:require [demo-works.db :as db]))

(defn add-todo
  "Given a user and a list, add a todo"
  [user-id list-id todo]
  (when (and todo 
             (not= "" todo)
             (db/retrieve-list user-id list-id))
  (let [record {:list_id list-id :task todo :state ":todo"}]
    (db/insert-todo record))))

(defn toggle-state [state] (if (= ":todo" state) ":done" ":todo"))

(defn toggle-todo
  "Given a user and a todo, toggle its state from done to todo and back"
  [user-id id]
  (some-> (db/retrieve-todo user-id id)
          (update :state toggle-state)
          db/update-todo))

(defn delete-todo
  "Delete a user's todo"
  [user-id id]
  (when (db/retrieve-todo user-id id)
    (db/delete-todo id)))

(defn get-lists-by-user
  "Retrieve all lists a user has access to"
  [user-id]
  (->> user-id
       db/retrieve-lists
       (map (fn [l] 
              (assoc l :todos (db/retrieve-todos user-id (:id l)))))))
