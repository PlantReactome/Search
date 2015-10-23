# Search
#### What is Reactome Search ?
Reactome Search is a project that optimizes the 'searches' in the Reactome Website. Based on Apache Lucene Solr, the database is indexed by Apache Solr 5.3.1 and it is fully configured and parametrized accordingly to meet the our needs and requirements.
The Search project is spit into 'Indexer' and 'Search':
  * Indexer: query GKInstance (MySql database) and indexer the whole database in Solr Model
  * Search: Spring MVC Application which queries Solr documents in order to optimize the searching for Reactome Pathway Browser.

#### How do I easily setup a new Apache Solr instance for Reactome ?
  <ol>
  <li>Download all-in-one script setup https://github.com/reactome/Search/blob/refactoring/install_solr.sh</li>
  <li>Open terminal</li>
  <li>Go to folder where the script has been downloaded</li>
  <li>Check script options before executing 
```
$> sudo ./install_solr.sh -h

OUTPUT:
install_solr.sh -i <password>  
                [-c <solr_core_name> 
                 -d <solr_home> 
                 —v <solr_version> 
                 -p <port> 
                 -u <user>] 
                 -- program to auto setup the Apache Lucene Solr in Reactome environment.
where:
    -h  Program help/usage
    -i  Solr Password
    -c  Solr Core name. DEFAULT: reactome
    -d  Solr Home directory. DEFAULT: /home/solr
    -v  Solr Version. DEFAULT: 5.3.1
    -p  Solr Port. DEFAULT: 8983
    -u  Solr User. DEFAULT: admin
```
  </li>
  <li>
      Execute script as root. 
      **You may need to specify a Solr Password. Please write down - this is mandatory for reaching out the Solr Console Site.**
      Replace the default arguments if necessary...

```
$> sudo ./install_solr.sh -i <password>
```

or

```
$> sudo ./install_solr.sh -i not4hack -p 8081 -u solruser
```

  </li>
  <li>To validate the installation of the Apache Solr the URL http://[serverip]:[port]/solr must ask for Basic Authentication. Please provide the user and password configured in the install_solr.sh script</li>
  <li> You're now able to run the Reactome Indexer. Follow next steps.</li>
  
</ol>
  
#### How do I run the Reactome Indexer ?
** Maven Setup: https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html.
<ol>
<li>Clone the Search repository on your end

```
git clone https://github.com/reactome/Search.git
```

</li>
<li>Navigate into indexer</li>
<li>Package with Maven

```
mvn clean package
```

</li>
<li>Navigate into target directory</li>
<li>
Run indexer

```
java -jar Indexer-[version]-jar-with-dependencies.jar -d dbname -u dbuser -p dbpass -s http://solrurl:solrport/solr/solrcore -c controlledvocabulary.csv -o ebeye.xml -r reactomedata-currentversio -v
```

e.g

```
java -jar Indexer-[version]-jar-with-dependencies.jar -d reactome -u reactome -p reactome -s http://localhost:8983/solr/reactome_final -c controlledvocabulary.csv -o ebeye.xml -r 49 -v
```

</li>
</ol>