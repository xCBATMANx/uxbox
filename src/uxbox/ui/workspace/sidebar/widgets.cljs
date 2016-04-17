;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2015-2016 Juan de la Cruz <delacruzgarciajuan@gmail.com>
;; Copyright (c) 2015-2016 Jesús Espino <jespinog@gmail.com>

(ns uxbox.ui.workspace.sidebar.widgets
  (:require [sablono.core :as html :refer-macros [html]]
            [rum.core :as rum]
            [lentes.core :as l]
            [uxbox.locales :refer (tr)]
            [uxbox.router :as r]
            [uxbox.rstore :as rs]
            [uxbox.state :as st]
            [uxbox.shapes :as shapes]
            [uxbox.library :as library]
            [uxbox.util.data :refer (read-string)]
            [uxbox.data.workspace :as dw]
            [uxbox.ui.workspace.base :as wb]
            [uxbox.ui.shapes.core :as uusc]
            [uxbox.ui.icons :as i]
            [uxbox.ui.mixins :as mx]
            [uxbox.util.dom :as dom]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Widgets Box
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widgets-render
  [own]
  (let [local (:rum/local own)
        on-close #(rs/emit! (dw/toggle-flag :widgets))]
    (html
     [:div#form-figures.tool-window
      [:div.tool-window-bar
       [:div.tool-window-icon i/puzzle]
       [:span "Widgets"]
       [:div.tool-window-close {:on-click on-close} i/close]]
      [:div.tool-window-content
       [:div.figures-catalog
        ;; extract component: set selector
        [:select.input-select.small
           [:option {:key :project-widgets :value :project-widgets} "Project Widgets"]
           [:option {:key :standard-widgets :value :standard-widgets} "Standard Widgets"]
           [:option {:key :your-widgets :value :your-widgets} "Your Widgets"]]]]])))

(def ^:static widgets-toolbox
  (mx/component
   {:render widgets-render
    :name "widgets"
    :mixins [mx/static rum/reactive]}))
