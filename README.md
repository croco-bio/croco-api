croco-api
=========

The CroCo regulatory network framework

Install maven package
mvn install -Dmaven.test.skip=true

Create bundled jar (including dependencies) 
mvn package

Copy java dependencies to target
mvn dependency:copy-dependencies

Generate javadoc
mvn javadoc:javadoc

Test the web-interface:
=========

Get the interface version:
curl -d "<object-stream/>" http://141.84.2.12/croco-web/services/plain/getVersion
curl -d "<object-stream/>" http://services.bio.ifi.lmu.de/croco-web/services/plain/getVersion

List the networks:
curl -d "<object-stream><string>/</string></object-stream>" http://141.84.2.12/croco-web/services/plain/getNetworkHierachy
GZIP compressed response:
curl -d "<object-stream><string>/</string></object-stream>" http://141.84.2.12/croco-web/services/getNetworkHierachy

Read specific network (e.g. network with ID: 1149):
curl -d "<object-stream><int>1149</int><null/><boolean>false</boolean></object-stream>" http://141.84.2.12/croco-web/services/plain/readNetwork


