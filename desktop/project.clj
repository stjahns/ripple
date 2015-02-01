(defproject ripple "0.0.1-SNAPSHOT"
  :description "FIXME: write description"

  :dependencies [[com.badlogicgames.gdx/gdx "1.1.0"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.1.0"]
                 [com.badlogicgames.gdx/gdx-box2d "1.1.0"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.1.0"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.1.0"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.1.0"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.1.0"
                  :classifier "natives-desktop"]
                 [org.clojure/clojure "1.6.0"]
                 [circleci/clj-yaml "0.5.3"]
                 [play-clj "0.3.6"]
                 [brute "0.3.0"
                  :exclusions [org.clojure/clojure]]
                 [aprint "0.1.3"]]

  :source-paths ["src" "src-common"]
  :resource-paths ["resources"]
  :test-paths ["test"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [ripple.core.desktop-launcher]
  :main ripple.core.desktop-launcher)
