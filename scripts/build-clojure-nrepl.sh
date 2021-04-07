cd nrepl-clojure
clojure -X:depstar uberjar :jar target/nrepl-clojure.jar
mvn install:install-file -Dfile=target/nrepl-clojure.jar -DpomFile=pom.xml
