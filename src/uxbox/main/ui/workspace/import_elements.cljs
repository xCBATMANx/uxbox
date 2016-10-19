;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2016 Juan de la Cruz <delacruzgarciajuan@gmail.com>

(ns uxbox.main.ui.workspace.import-elements
  (:require [cuerdas.core :as str]
            [promesa.core :as p]
            [uxbox.main.data.lightbox :as udl]
            [uxbox.main.exports :as exports]
            [uxbox.main.state :as st]
            [uxbox.main.ui.icons :as i]
            [uxbox.main.ui.lightbox :as lbx]
            [uxbox.main.ui.workspace.base :as wb]
            [uxbox.util.blob :as blob]
            [uxbox.util.data :refer (read-string)]
            [uxbox.util.datetime :as dt]
            [uxbox.util.dom :as dom]
            [uxbox.util.mixins :as mx :include-macros true]
            [uxbox.util.rstore :as rs]
            [uxbox.util.zip :as zip]
            [lentes.core :as l]))

(mx/defcs import-elements-dialog
  {:mixins [mx/static]}
  [own]
  [:div.lightbox-body.import-elements
   [:h3 "Save icon or widget"]
   [:div.row-flex
    [:div.content-col
     [:div.image-placeholder i/logo-icon]]
    [:div.content-col.options
     [:input.input-text {:type "text" :placeholder "New icon or widget name"}]
     [:div.input-columns
      [:div.input-radio.radio-primary
       [:span "Save to"]
       [:input {:type "radio" :id "save-to" :name "save-to" :value "project"}]
       [:label {:for "save-to" :value "project"} "Current project"]
       [:input {:type "radio" :id "save-to-2" :name "save-to-2" :value "Library"}]
       [:label {:for "save-to-2" :value "Library"} "Library"]]
      [:div.input-radio.radio-primary
       [:span "Save from"]
       [:input {:type "radio" :id "save-from" :name "save-from" :value "page"}]
       [:label {:for "save-from" :value "page"} "Whole page"]
       [:input {:type "radio" :id "save-from-2" :name "save-from-2" :value "selection"}]
       [:label {:for "save-from-2" :value "selection"} "Selection"]]]
       [:select.input-select {:ref "library" :default-value "Choose a library"}
       [:option {:value "Library 1"}]
       [:option {:value "Library 2"}]]
     [:a.btn-primary {:href "#"} "Save it!"]]
   [:a.close {:href "#"} i/close]]])

(defmethod lbx/render-lightbox :import-elements
  [_]
  (import-elements-dialog))
