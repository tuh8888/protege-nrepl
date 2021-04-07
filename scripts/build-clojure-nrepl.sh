cd nrepl-clojure
clojure -X:depstar uberjar :jar target/nrepl-clojure.jar
mvn install:install-file -Dfile=target/nrepl-clojure.jar -DgroupId=ucdenver.ccp -DartifactId=nrepl-clojure -Dversion=0.2.0-SNAPSHOT -Dpackaging=jar
