(ns demo-works.view
  (:require [demo-works.domain :refer :all]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [hiccup.element :refer [link-to]]))

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
  [user]
  (let [lists (get-lists-by-user (:id user))]
    (html [:div
           [:h1 (str "Hello " (:name user))]
           (link-to "/logout" "Logout")
           (format-lists lists)])))

(defn login-page
  [users]
  (html [:div
         [:h1 "User Login"]
         [:ul
          (for [user users]
            [:li
             (link-to (str "/login/" (:id user)) (:name user))])]]))
  
(defn about-page
  []
  (html [:h1 "About"]
        [:p "This project is to demonstrate a basic web application to the phenomenal staff of Democracy Works!"]))


