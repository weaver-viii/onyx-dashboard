(ns onyx-dashboard.notifications)

(defn success-notification [msg]
  (js/noty (clj->js {:text msg
                     :type "success"
                     :layout "bottomRight"
                     :timeout 8000
                     :closeWith ["click" "button"]})))
