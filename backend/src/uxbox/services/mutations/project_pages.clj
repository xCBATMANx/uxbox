;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2019 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.mutations.project-pages
  (:require
   [clojure.spec.alpha :as s]
   [promesa.core :as p]
   [uxbox.db :as db]
   [uxbox.services.mutations :as sm]
   [uxbox.services.mutations.project-files :as files]
   [uxbox.services.queries.project-pages :refer [decode-row]]
   [uxbox.services.util :as su]
   [uxbox.util.blob :as blob]
   [uxbox.util.spec :as us]
   [uxbox.util.sql :as sql]
   [uxbox.util.uuid :as uuid]))

;; --- Helpers & Specs

;; TODO: validate `:data` and `:metadata`

(s/def ::id ::us/uuid)
(s/def ::name ::us/string)
(s/def ::data any?)
(s/def ::user ::us/uuid)
(s/def ::project-id ::us/uuid)
(s/def ::metadata any?)
(s/def ::ordering ::us/number)

;; --- Mutation: Create Page

(declare create-page)

(s/def ::create-project-page
  (s/keys :req-un [::user ::file-id ::name ::metadata ::data]
          :opt-un [::id]))

(sm/defmutation ::create-project-page
  [{:keys [user file-id] :as params}]
  (db/with-atomic [conn db/pool]
    (files/check-edition-permissions! conn user file-id)
    (create-page conn params)))

(defn create-page
  [conn {:keys [id user file-id name ordering data metadata] :as params}]
  (let [sql "insert into project_pages (id, user_id, file_id, name,
                                        ordering, data, metadata, version)
             values ($1, $2, $3, $4, $5, $6, $7, 0)
             returning *"
        id   (or id (uuid/next))
        data (blob/encode data)
        mdata (blob/encode metadata)]
    (-> (db/query-one conn [sql id user file-id name ordering data mdata])
        (p/then' decode-row))))

;; --- Mutation: Update Page

(declare select-page-for-update)
(declare update-page)
(declare update-history)

(s/def ::update-project-page-data
  (s/keys :req-un [::id ::user ::name ::data]))

(sm/defmutation ::update-project-page-data
  [{:keys [id user data] :as params}]
  (db/with-atomic [conn db/pool]
    (p/let [{:keys [version file-id]} (select-page-for-update conn id)]
      (files/check-edition-permissions! conn user file-id)
      (let [data (blob/encode data)
            version (inc version)
            params (assoc params :id id :data data)]
        (p/do! (update-page conn params)
               (update-history conn params)
               (select-keys params [:id :version]))))))

(defn- select-page-for-update
  [conn id]
  (let [sql "select p.id, p.version, p.file_id
               from project_pages as p
              where p.id = $1
                and deleted_at is null
                 for update;"]
    (-> (db/query-one conn [sql id])
        (p/then' su/raise-not-found-if-nil))))

(defn- update-page
  [conn {:keys [id name version data metadata user]}]
  (let [sql "update project_pages
                set name = $1,
                    version = $2,
                    data = $3,
              where id = $4"]
    (-> (db/query-one conn [sql name version data id])
        (p/then' su/constantly-nil))))

(defn- update-history
  [conn {:keys [user id version data]}]
  (let [sql "insert into project_page_history (user_id, page_id, version, data)
             values ($1, $2, $3, $4)"]
    (-> (db/query-one conn [sql user id version data])
        (p/then' su/constantly-nil))))

;; --- Mutation: Rename Page

(declare rename-page)

(s/def ::rename-project-page
  (s/keys :req-un [::id ::name ::user]))

(sm/defmutation ::rename-project-page
  [{:keys [id name user]}]
  (db/with-atomic [conn db/pool]
    (p/let [page (select-page-for-update conn id)]
      (files/check-edition-permissions! conn user (:file-id page))
      (rename-page conn (assoc page :name name)))))

(defn- rename-page
  [conn {:keys [id name] :as params}]
  (let [sql "update project_pages
                set name = $2
              where id = $1
                and deleted_at is null"]
    (-> (db/query-one db/pool [sql id name])
        (p/then su/constantly-nil))))

;; --- Mutation: Update Page Metadata

;; (s/def ::update-page-metadata
;;   (s/keys :req-un [::user ::project-id ::name ::metadata ::id]))

;; (sm/defmutation ::update-page-metadata
;;   [{:keys [id user project-id name metadata]}]
;;   (let [sql "update pages
;;                 set name = $3,
;;                     metadata = $4
;;               where id = $1
;;                 and user_id = $2
;;                 and deleted_at is null
;;              returning *"
;;         mdata (blob/encode metadata)]
;;     (-> (db/query-one db/pool [sql id user name mdata])
;;         (p/then' decode-row))))

;; --- Mutation: Delete Page

(declare delete-page)

(s/def ::delete-project-page
  (s/keys :req-un [::user ::id]))

(sm/defmutation ::delete-project-page
  [{:keys [id user]}]
  (db/with-atomic [conn db/pool]
    (p/let [page (select-page-for-update conn id)]
      (files/check-edition-permissions! user (:file-id page))
      (delete-page conn user id))))

(defn- delete-page
  [conn user id]
  (let [sql "update project_pages
                set deleted_at = clock_timestamp()
              where id = $1
                and deleted_at is null"]
    (-> (db/query-one db/pool [sql id user])
        (p/then su/constantly-nil))))

;; --- Update Page History

;; (defn update-page-history
;;   [conn {:keys [user id label pinned]}]
;;   (let [sqlv (sql/update-page-history {:user user
;;                                        :id id
;;                                        :label label
;;                                        :pinned pinned})]
;;     (some-> (db/fetch-one conn sqlv)
;;             (decode-row))))

;; (s/def ::label ::us/string)
;; (s/def ::update-page-history
;;   (s/keys :req-un [::user ::id ::pinned ::label]))

;; (sm/defmutation :update-page-history
;;   {:doc "Update page history"
;;    :spec ::update-page-history}
;;   [params]
;;   (with-open [conn (db/connection)]
;;     (update-page-history conn params)))
