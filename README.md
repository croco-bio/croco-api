General
=========
CroCo provides a new view on regulatory networks (transcription factor-target) for many species derived from context-specific ENCODE projects, the scientific literature and structured databases. Networks can be easily retrieved from a remote croco network repository (croco-repo) and combined via many networks operations.

consists of five components:

1. a network repository croco-repo,
2. an Application Programming Interface (API): croco-api
3. a Cytoscape plug-in: croco-cyto
4. a web application: croco-web
5. a web-service for remote access to the central repository: croco-service

See also: http://services.bio.ifi.lmu.de/croco-web/

croco-api
=========
The croco-api provides functionalities to query a croco-repository (local and remote!), to perform network operations and to construct networks from (raw) data.

croco-api installation
=========
see http://services.bio.ifi.lmu.de/croco-web for latest (stable) build.

croco-api build (from source)
=========

Install maven package
mvn install -Dmaven.test.skip=true

Create bundled jar (including dependencies) 
mvn package

Copy java dependencies to target
mvn dependency:copy-dependencies

Generate javadoc
mvn javadoc:javadoc

Access to the remote  (java with croco-api)
=========

Instansiate a remote web service object:
```Java
RemoteWebService remoteService = new RemoteWebService("http://services.bio.ifi.lmu.de/croco-web/services/");
```

Retrieve the network ontology:
```Java
CroCoNode root =service.getNetworkOntology();
```

Finding specific networks in the repository:
```Java

GeneralFilter f1 = new GeneralFilter(Option.TaxId,9606+"");
GeneralFilter f2 = new GeneralFilter(Option.NetworkType,NetworkType.OpenChrom.name());
GeneralFilter f3 = new GeneralFilter(Option.ConfidenceThreshold,"1.0E-6");
GeneralFilter f4 = new GeneralFilter(Option.MotifSet,"Combined set");
GeneralFilter f5 = new GeneralFilter(Option.OpenChromType,"DNase");
GeneralFilter f6 = new GeneralFilter(Option.cellLine,"K562");

CroCoNode k562 = root.getNode("MEL", f1,f2,f3,f4,f5,f6);
	
List<NetworkHierachyNode> k562Networks = new ArrayList<NetworkHierachyNode>(k562.getNetworks());
```

Read networks
```Java
for(NetworkHierachyNode nh :k562Networks)
{
	ReadNetwork reader = new ReadNetwork();
	reader.setInput(ReadNetwork.QueryService, service);
	reader.setInput(ReadNetwork.NetworkHierachyNode,nh);
			
	Network network = reader.operate();
		
	//get edges
	for(int edgeId : network.getEdgeIds()){
    	Tuple<Entity, Entity> edge = net.getEdge(edgeId);	
    }
}
```

Access to the croco-web service (bash)
=========
Via the croco-service a remote croco database with many pre-compiled networks can be queried.

Get the remote (croco-api) version:
```Shell
curl -d "<object-stream/>" http://141.84.2.12/croco-web/services/plain/getVersion
curl -d "<object-stream/>" http://services.bio.ifi.lmu.de/croco-web/services/plain/getVersion
```

List the networks:

```Shell
curl -d "<object-stream> <boolean>true</boolean></object-stream>" http://141.84.2.12/croco-web/services/plain/getNetworkOntology
GZIP compressed response:
curl -d "<object-stream> <boolean>true</boolean></object-stream>" http://141.84.2.12/croco-web/services/getNetworkOntology
```


Read specific network (e.g. network with ID: 1149):
```Shell
curl -d "<object-stream><int>1149</int><null/><boolean>false</boolean></object-stream>" http://141.84.2.12/croco-web/services/plain/readNetwork
```
Note
=========
The newest source code may not always be compatible with the remote croco instance. For croco-service compatible versions see: http://services.bio.ifi.lmu.de/croco-web.

Contact & Bug-reports & Suggestions
=========
We are constantly improving the croco components. So if you find any bugs, or have general suggestions/comments, please send a mail to: robert.pesch@bio.ifi.lmu.de
