;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2019 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.mutations.project-files
  (:require
   [clojure.spec.alpha :as s]
   [promesa.core :as p]
   [uxbox.db :as db]
   [uxbox.util.spec :as us]
   [uxbox.services.mutations :as sm]
   [uxbox.services.mutations.projects :as proj]
   [uxbox.services.util :as su]
   [uxbox.util.blob :as blob]
   [uxbox.util.uuid :as uuid]))

;; --- Helpers & Specs

(s/def ::id ::us/uuid)
(s/def ::name ::us/string)
(s/def ::user ::us/uuid)
(s/def ::project-id ::us/uuid)

;; --- Permissions Checks

;; A query that returns all (not-equal) user assignations for a
;; requested file (project level and file level).

;; Is important having the condition of user_id in the join and not in
;; where clause because we need all results independently if value is
;; true, false or null; with that, the empty result means there are no
;; file found.

(def ^:private sql:file-permissions
  "select pf.id,
          pfu.can_edit as can_edit
     from project_files as pf
     left join project_file_users as pfu
       on (pfu.file_id = pf.id and pfu.user_id = $1)
    where pf.id = $2
   union all
   select pf.id,
          pu.can_edit as can_edit
     from project_files as pf
     left join project_users as pu
       on (pf.project_id = pu.project_id and pu.user_id = $1)
    where pf.id = $2")

(defn check-edition-permissions!
  [conn user file-id]
  (-> (db/query conn [sql:project-permissions file-id user])
      (p/then' (comp su/raise-not-found-if-nil seq))
      (p/then' (fn [rows]
                 (when-not (some :can-edit rows)
                   (ex/raise :type :validation
                             :code :not-authorized))))))

;; --- Mutation: Create Project

(declare create-project-file)
(declare create-project-page)

(s/def ::create-project-file
  (s/keys :req-un [::user ::name ::project-id]
          :opt-un [::id]))

(sm/defmutation ::create-project-file
  [{:keys [project-id] :as params}]
  (db/with-atomic [conn db/pool]
    (proj/check-edition-permissions! conn user project-id)
    (-> (create-project-file conn params)
        (p/then #(create-project-page conn (assoc params :file-id %))))))

(defn- create-project-file
  [conn {:keys [id user name project-id] :as params}]
  (let [id (or id (uuid/next))
        sql "insert into project_files (id, user_id, project_id, name)
             values ($1, $2, $3, $4) returning id"]
    (-> (db/query-one conn [sql id user project-id name])
        (p/then' :id))))

(defn- create-project-page
  "Creates an initial page for the file."
  [conn {:keys [user file-id] :as params}]
  (let [id  (uuid/next)
        name "Page 1"
        sql "insert into project_pages (id, user_id, file_id, name)
             values ($1, $2, $3, $4) returning id"]
    (db/query-one conn [sql id user file-id name])))

;; --- Mutation: Update Project

(declare update-project-file)

(s/def ::update-project
  (s/keys :req-un [::user ::name ::id]))

(sm/defmutation ::update-project-file
  [{:keys [id user] :as params}]
  (db/with-atomic [conn db/pool]
    (check-edition-permissions! conn user id)
    (update-project-file conn params)))

(defn- update-project-file
  [conn {:keys [id name user] :as params}]
  (let [sql "update project_files
                set name = $2
              where id = $1
                and deleted_at is null"]
    (-> (db/query-one conn [sql id user name])
        (p/then' su/constantly-nil))))

;; --- Mutation: Delete Project

(declare delete-project-file

(s/def ::delete-project-file
  (s/keys :req-un [::id ::user]))

(sm/defmutation ::delete-project-file
  [{:keys [id user] :as params}]
  (db/with-atomic [conn db/pool]
    (check-edition-permissions! conn user id)
    (delete-project-file conn params)))

(def ^:private sql:delete-project-file
  "update project_file
      set deleted_at = clock_timestamp()
    where id = $1
      and deleted_at is null
   returning id")

(defn delete-project-file
  [conn {:keys [id user] :as params}]
  (let [sql sql:delete-project-file]
    (-> (db/query-one db/pool [sql id user])
        (p/then' su/constantly-nil))))
