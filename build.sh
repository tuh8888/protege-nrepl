## build just the plugin and dump it to protege

RELEASE_VERSION=0.2.0

cd server
rm pom.xml
clojure -X:depstar uberjar \
        :version \"${RELEASE_VERSION}\" \
        :jar \"target/protege-nrepl-server-${RELEASE_VERSION}.jar\"
mvn install:install-file -Dfile=target/protege-nrepl-server-${RELEASE_VERSION}.jar -DpomFile=pom.xml
cd ..

cd plugin
mvn -DskipTests=true install
cd ..

rm /opt/protege/plugins/protege-nrepl*.jar
cp plugin/target/protege-nrepl-${RELEASE_VERSION}.jar /opt/protege/plugins/

protege
