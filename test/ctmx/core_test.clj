(ns ctmx.core-test
  (:require [clojure.test :refer :all]
            [ctmx.core :as ctmx :refer [defcomponent]]
            reitit.ring
            [ring.mock.request :as mock]))

;; helper functions

(defn- test-req [raw-handler req]
  ((-> raw-handler reitit.ring/router reitit.ring/ring-handler) req))

(defn- redirect? [^String redirect-path {:keys [status headers]}]
  (and
   (= 302 status)
   headers
   (-> "Location" headers (= redirect-path))))

(defn- ok? [^String test-body {:keys [status body]}]
  (and
   (= 200 status)
   (= test-body body)))

(defn- no-content? [{:keys [status]}]
  (= 204 status))

;; end helper functions

(defcomponent ^:endpoint c [req])
(defcomponent ^:endpoint a [req]
  [:a])
(defcomponent b [req]
  c
  [:b (a req)])

(def handler (ctmx/make-routes
              "/base"
              (fn [req]
                (b req))))

(deftest component-test
  (testing "redirects to slash ending"
           (is
            (redirect?
             "/base/?"
             (test-req
              handler
              (mock/request :get "/base")))))
  (testing "initial render works"
           (is
            (= [:b [:a]]
               (test-req
                handler
                (mock/request :get "/base/")))))
  (testing "a endpoint works"
           (is
            (ok?
             "<a></a>"
             (test-req
              handler
              (mock/request :get "/base/a")))))
  (testing "c endpoint works"
           (is
            (no-content?
             (test-req
              handler
              (mock/request :get "/base/c")))))
  (testing "no b endpoint"
           (is
            (nil?
             (test-req
              handler
              (mock/request :get "/base/b"))))))

