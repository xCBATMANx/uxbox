;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2019 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.services.init
  "A initialization of services."
  (:require
   [mount.core :as mount :refer [defstate]]))

(defn- load-query-services
  []
  #_(require 'uxbox.services.queries.icons)
  #_(require 'uxbox.services.queries.images)
  #_(require 'uxbox.services.queries.pages)
  #_(require 'uxbox.services.queries.profiles)
  #_(require 'uxbox.services.queries.projects)
  #_(require 'uxbox.services.queries.user-storage))

(defn- load-mutation-services
  []
  #_(require 'uxbox.services.mutations.auth)
  #_(require 'uxbox.services.mutations.icons)
  #_(require 'uxbox.services.mutations.images)
  #_(require 'uxbox.services.mutations.projects)
  #_(require 'uxbox.services.mutations.pages)
  #_(require 'uxbox.services.mutations.profiles)
  #_(require 'uxbox.services.mutations.user-storage))

(defstate query-services
  :start (load-query-services))

(defstate mutation-services
  :start (load-mutation-services))
