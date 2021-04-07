cd server
clojure -X:depstar uberjar \
        :version \"${RELEASE_VERSION}\" \
        :jar \"target/protege-nrepl-server-${RELEASE_VERSION}.jar\"
mvn install:install-file -Dfile=target/protege-nrepl-server-${RELEASE_VERSION}.jar -DpomFile=pom.xml
