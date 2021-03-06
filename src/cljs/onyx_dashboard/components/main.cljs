(ns onyx-dashboard.components.main
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-bootstrap.grid :as g]
            [om-bootstrap.random :as r]
            [onyx-dashboard.components.deployment :refer [select-deployment deployment-indicator 
                                                          deployment-peers deployment-log-dump]]
            [onyx-dashboard.components.jobs :refer [job-selector job-info job-management]]
            [onyx-dashboard.components.log :refer [log-entries-pager]]
            [onyx-dashboard.controllers.api :refer [api-controller]]
            [cljs.core.async :as async :refer [<! >! put! chan]])
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]]))

(defcomponent main-component [{:keys [deployment metrics] :as app} owner]
  (did-mount [_] 
             (let [api-ch (om/get-shared owner :api-ch)
                   chsk-send! (om/get-shared owner :chsk-send!)] 
               (go-loop []
                        (let [msg (<! api-ch)]
                          (om/transact! app (partial api-controller msg chsk-send!))
                          (recur)))))

  (render-state [_ {:keys [api-chan]}]
                (let [{:keys [selected-job jobs]} deployment
                      job (and selected-job jobs (jobs selected-job))] 
                  (dom/div
                    (r/page-header {:class "page-header, main-header"}
                                   (g/grid {}
                                           (g/col {:xs 6 :md 4}
                                                  (dom/a {:href "https://github.com/MichaelDrogalis/onyx"}
                                                         (dom/img {:class "logo" 
                                                                   :src "/img/high-res.png" 
                                                                   :height 60})))
                                           (g/col {:xs 12 :md 8}
                                                  (dom/div {:style {:font-size "42px" 
                                                                    :margin-top "10px"}}
                                                           "Onyx Dashboard"))))
                    (g/grid {}
                            (g/row {}
                                   (g/col {:xs 6 :md 4}
                                          (dom/div {:class "left-nav-deployment"} 
                                                   (om/build select-deployment app {}))
                                          (if (:id deployment) 
                                            (dom/div 
                                              (om/build deployment-indicator 
                                                        {:deployment deployment
                                                         :last-entry ((:entries deployment) (:message-id-max deployment))})
                                              (om/build job-selector deployment {})
                                              (om/build deployment-peers deployment {})
                                              (if job 
                                                (om/build job-management 
                                                          job
                                                          {:react-key (str "management-" (:id job))}))
                                              (om/build deployment-log-dump deployment))))
                                   (g/col {:xs 11 :md 8}
                                          (if (:id deployment) 
                                            (dom/div 
                                              (if job 
                                                (om/build job-info   
                                                          (if (:up? deployment)
                                                            {:job job :metrics metrics}
                                                            {:job (dissoc job :tasks :peers)})
                                                          {:react-key (str "job-info-" (:id job))}))

                                              (om/build log-entries-pager 
                                                        {:entries (:entries deployment)
                                                         :job-filter (:id job)} 
                                                        {:react-key (str "log-" (:id deployment) "-filter-" (:id job))}))))))))))
