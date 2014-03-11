croco-api
=========

The CroCo regulatory network framework

Install
mvn install -Dmaven.test.skip=true

Create bundled jar (including dependencies) 
mvn package

Copy java dependencies to target
mvn dependency:copy-dependencies

Generate javadoc
mvn javadoc:javadoc