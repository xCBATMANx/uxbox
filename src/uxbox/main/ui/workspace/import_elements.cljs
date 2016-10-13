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
  [:div.lightbox-body.export-dialog
   [:h3 "Save icon | widget options"]
   [:div.row-flex
    [:div.content-col
     [:span.icon i/file-svg]
     [:span.title "Export page"]
     [:p.info "Download a single page of your project in SVG."]
     [:select.input-select {:ref "page" :default-value "Hola mundo"}
      [:option {:value "Hola mundo 2"}]
     [:a.btn-primary {:href "#"} "Export page"]]
    [:div.content-col
     [:span.icon i/folder-zip]
     [:span.title "Export project"]
     [:p.info "Download the whole project as a ZIP file."]
     [:a.btn-primary {:href "#"} "Expor project"]]
    [:div.content-col
     [:span.icon i/file-html]
     [:span.title "Export as HTML"]
     [:p.info "Download your project as HTML files."]
     [:a.btn-primary {:href "#"} "Export HTML"]]]
   [:a.close {:href "#"} i/close]]])

(defmethod lbx/render-lightbox :import-elements
  [_]
  (import-elements-dialog))
