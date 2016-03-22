(ns demo-works.domain-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [demo-works.db :as db]
            [demo-works.domain :as d]
            [demo-works.core :refer :all]))

(defn- exec [cmd] (sql/execute! @db/db-spec cmd))

(defn- create-db-tables
  []
  (exec ["create table user(id integer primary key autoincrement not null, name text not null)"])
  (exec ["create table list(id integer primary key autoincrement not null, name text not null)"])
  (exec ["create table list_acl(id integer primary key autoincrement not null, list_id integer not null, user_id integer not null, role text not null)"])
  (exec ["create table todo(id integer primary key autoincrement not null, list_id integer not null, task text not null, state text not null)"]))

(defn- drop-db-tables
  []
  (exec ["drop table if exists user"])
  (exec ["drop table if exists list"])
  (exec ["drop table if exists list_acl"])
  (exec ["drop table if exists todo"]))

(defn- setup-data
  []
  (exec ["insert into user (id, name) values (?,?)" 1 "Dad"])
  (exec ["insert into user (id, name) values (?,?)" 2 "Mom"])
  (exec ["insert into list (id, name) values (?,?)" 1 "Groceries"])
  (exec ["insert into list (id, name) values (?,?)" 2 "Books"])
  (exec ["insert into list_acl (id, list_id, user_id, role) values (?,?,?,?)" 1 1 1 ":owner"])
  (exec ["insert into list_acl (id, list_id, user_id, role) values (?,?,?,?)" 2 1 2 ":collaborator"])
  (exec ["insert into list_acl (id, list_id, user_id, role) values (?,?,?,?)" 3 2 2 ":owner"])
  (exec ["insert into todo (id, list_id, task, state) values (?,?,?,?)" 1 1 "Coffee" ":todo"])
  (exec ["insert into todo (id, list_id, task, state) values (?,?,?,?)" 2 2 "Slaughterhouse Five" ":todo"]))

(defn- db-get-todo [id] (first (sql/query @db/db-spec ["select * from todo where id = ?" id])))

(use-fixtures :once (fn [f]
                      (db/init-db "testing.db")
                      (f)))

(use-fixtures :each (fn [f]
                      (drop-db-tables)
                      (create-db-tables)
                      (setup-data)
                      (f)))

(deftest retrieval
  (testing "retrieve all users"
    (let [users (d/all-users)]
      (is (= 2 (count users)))
      (is (= "Dad" (-> users first :name)))))

  (testing "retrieve individual user"
    (let [user (d/user 1)]
      (is (= "Dad" (:name user)))))

  (testing "retrieve user's lists"
    (let [dad (d/user-lists 1)
          mom (d/user-lists 2)]
      (is (= 1 (count dad)))
      (is (= "Groceries" (-> dad first :name)))
      (is (= 2 (count mom)))
      (is (= "Books" (-> mom second :name)))))

  (testing "retrieve list-todos"
    (let [grocery-list (first (d/user-lists 1))]
      (is (= 1 (count (:todos grocery-list))))
      (is (= "Coffee" (-> grocery-list :todos first :task))))))

(deftest complete-todo
  (testing "complete todo"
    (d/toggle-todo 1 1)
    (let [todo (db-get-todo 1)]
      (is (= ":done" (:state todo)))))
  (testing "cannot complete todo that isn't mine"
    (d/toggle-todo 1 2)
    (let [todo (db-get-todo 2)]
      (is (= ":todo" (:state todo))))))

(deftest undo-complete-todo
  (testing "undo completed todo"
    (d/toggle-todo 1 1)
    (d/toggle-todo 1 1)
    (let [todo (db-get-todo 1)]
      (is (= ":todo" (:state todo))))))

(deftest add-todo
  (testing "add a new todo to a list"
    (d/add-todo 1 1 "I can't believe it's not butter")
    (let [todo (db-get-todo 3)]
      (is (= "I can't believe it's not butter" (:task todo)))
      (is (= ":todo" (:state todo))))))

(deftest delete-todo
  (testing "delete an existing todo I can access"
    (d/delete-todo 1 1)
    (is (nil? (db-get-todo 1))))
  (testing "deleting a todo is disallowed"
    (d/delete-todo 1 2)
    (is (= ":todo" (-> (db-get-todo 2) :state)))))
