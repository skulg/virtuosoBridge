package virtuosoBridge;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;

import virtuoso.jena.driver.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.jena.base.Sys;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
public class WikiQuerier {
	private  String user="";
	private  String pass="";
	private  String server="";

	private VirtGraph set ;
	private String prefix="PREFIX relation: <http://test/relation/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX term: <http://test/term/> PREFIX cat: <http://test/category/>";
	private String select = "";
	private String where ="";
	private String params="";
	private String graph="";


	public WikiQuerier(String user,String pass, String server , String graph){
		this.user=user;
		this.pass=pass;
		this.server=server;
		this.graph=graph;
		set=new VirtGraph(this.graph,"jdbc:virtuoso://"+server+"/charset=UTF-8/log_enable=2", user, pass);

	}

	/*
	 * Print Whole DataSet
	 * 
	 */
	public void selectAll(){
		ArrayList<String> vars= new ArrayList<String>();
		vars.add("s");
		vars.add("p");
		vars.add("o");

		this.select="SELECT *";
		this.where="WHERE {?s ?p ?o }";
		this.params="";

		ResultSet results=this.runQuery();
		virtuosoBridgeTools.somewhatPrettyPrint(vars, results);

	}

	/*
	 * Return the relation profile of Entity corresponding to URI
	 */
	public HashMap<String, Double> findEntityRelationProfile2(String uri){
		int normalFactor=1;

		this.select="SELECT (count(?p) AS ?totalCount)";
		this.where="WHERE {"+uri +" ?p ?o }";
		this.params="";
		ResultSet tempResult = this.runQuery();

		if(tempResult.hasNext()){
			QuerySolution result = tempResult.nextSolution();
			RDFNode tCount= result.get("totalCount");
			normalFactor=Integer.valueOf(virtuosoBridgeTools.entityCleaner(tCount.toString()));
		}

		ArrayList<String> vars= new ArrayList<String>();	
		HashMap<String,Double> relationMap = new HashMap<String,Double>();
		vars.add("p");
		vars.add("pCount");
		this.select="SELECT ?p (xsd:float(count(?p))/"+normalFactor+ "AS ?pCount)";
		this.where="WHERE {"+uri +" ?p ?o }";
		this.params="GROUP BY ?p ORDER BY DESC(?pCount)";

		ResultSet results=this.runQuery();
		//somewhatPrettyPrint(vars, results);
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String relation=result.get("p").toString();
			Double prob=Double.valueOf(virtuosoBridgeTools.entityCleaner(result.get("pCount").toString()));
			relationMap.put(relation,prob);

		}
		return relationMap;	
	}

	/*
	 * Return the relation profile of Entity corresponding to URI
	 * 
	 * BUGGED right now since the graph as problem use findEntityRelationProfile2 for now
	 * 
	 */
	private HashMap<String, Double> findEntityRelationProfile(String uri){

		String relationProfileGraph="http://wikiDataRelProfile";

		ArrayList<String> vars= new ArrayList<String>();	
		HashMap<String,Double> relationMap = new HashMap<String,Double>();
		vars.add("p");
		vars.add("o");


		this.select="SELECT ?p ?o FROM <"+relationProfileGraph+"> ";
		this.where="WHERE {graph <"+relationProfileGraph +"> {"+uri +" ?p ?o }}";
		this.params="";

		ResultSet results=this.runQuery();

		virtuosoBridgeTools.resultSetToRelationMap(relationMap, results);


		return relationMap;	
	}

	/*
	 * Return sum of relations profiles according to some category and normalize
	 */
	public HashMap<String, Double> calcRelProfileFromCat(String uri){
		HashMap<String, Double> resultMap= new HashMap<String,Double>();
		LinkedList<String> list= new LinkedList<String>();

		System.out.println("Fetching entities");
		list=fetchAllEntitiesBelongingToCat(uri);


		System.out.println("Calculating Relations Profiles from List");
		resultMap=this.fetchSumNormalizeRelationsProfiles(list);


		return resultMap;
	}

	
	
	//Fetch cached relation graph belonging to URI category instead of calculating from terms.
	public HashMap<String, Double> fetchRelProfileFromGraph(String uri){
		
		String relationProfileGraph="http://wikiDataRelProfile";

		ArrayList<String> vars= new ArrayList<String>();	
		HashMap<String,Double> relationMap = new HashMap<String,Double>();
		vars.add("p");
		vars.add("o");


		this.select="SELECT ?p ?o FROM <"+relationProfileGraph+"> ";
		this.where="WHERE {graph <"+relationProfileGraph +"> {"+uri +" ?p ?o }}";
		this.params="";

		ResultSet results=this.runQuery();

		virtuosoBridgeTools.resultSetToRelationMap(relationMap, results);


		return relationMap;	
		
	}
	
	
	
	/*
	 * Return a list of URI belonging to category
	 */

	public LinkedList<String> fetchAllEntitiesBelongingToCat(String uri){
		LinkedList<String> resultList = new LinkedList<String>();
		ArrayList<String> vars= new ArrayList<String>();
		vars.add("a");
		this.select="SELECT ?a ";
		this.where="WHERE { ?a relation:être " +uri + " }";
		this.params="";

		ResultSet results=this.runQuery();
		//somewhatPrettyPrint(vars, results);
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String entity=result.get("a").toString();
			resultList.add(entity);

		}



		return resultList;
	}


	

	/*
	 *  Fetch relations profiles of List and sum them up and normalize on the way
	 */

	 private HashMap<String, Double> fetchSumNormalizeRelationsProfiles(LinkedList<String> list){
		HashMap<String, Double> resultMap = new HashMap<String,Double>();

		Iterator<String> iter =list.iterator();
		int normalizingFactor=0;
		//FETCH first occurence out of loop to initialize resultMap
		if (iter.hasNext()){
			String currentUri="<"+iter.next()+">";
			HashMap<String, Double> currentMap =this.findEntityRelationProfile2(currentUri);
			resultMap=currentMap;
			resultMap=virtuosoBridgeTools.sum2RelationProfile(resultMap, currentMap);
			normalizingFactor++;

		}
		while (iter.hasNext()){
			String currentUri="<"+iter.next()+">";
			//System.out.println("");

			//System.out.println("Treating:" + currentUri);
			HashMap<String, Double> currentMap =this.findEntityRelationProfile2(currentUri);

			//System.out.println("Before MAP !!!!!!!");
			//this.printSortedRelationProfile(resultMap);
			//System.out.println("ADDING TOMAP !!!!!!!");

			//this.printSortedRelationProfile(currentMap);


			resultMap=virtuosoBridgeTools.sum2RelationProfile(resultMap, currentMap);
			//System.out.println("RESULT MAP !!!!!!!");

			//this.printSortedRelationProfile(resultMap);
			
			
			normalizingFactor++;

		}
		
		resultMap=virtuosoBridgeTools.normalizeRelationProfile(resultMap, normalizingFactor);

		return resultMap;
	}

	/*
	 * Compare 2 relationProfile for ordering
	 */

	public HashMap<String, Double> generateNormalRelationProfile(){
		int normalFactor=findNumbersOfTriplets();
		ArrayList<String> vars= new ArrayList<String>();	
		HashMap<String,Double> normalRelationMap = new HashMap<String,Double>();
		vars.add("p");
		vars.add("pCount");
		this.select="SELECT ?p (xsd:float(count(?p))/"+normalFactor+ "AS ?pCount)";
		this.where="WHERE {?s ?p ?o }";
		this.params="GROUP BY ?p ORDER BY DESC(?pCount)";
		ResultSet results=this.runQuery();
		//somewhatPrettyPrint(vars, results);
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String relation=result.get("p").toString();

			Double prob=Double.valueOf(virtuosoBridgeTools.entityCleaner(result.get("pCount").toString()));
			normalRelationMap.put(relation,prob);

		}
		return normalRelationMap;	
	}

	/*
	 *  Return number of triplets in dataSet
	 */

	private int findNumbersOfTriplets(){
		return set.getCount();
	}

	/*
	 *  Print most frequent Relations
	 */

	public void selectMostFreqRelations(int limit){
		ArrayList<String> vars= new ArrayList<String>();	
		vars.add("p");
		vars.add("pCount");
		this.select="SELECT ?p (count(?p) AS ?pCount)";
		this.where="WHERE {?s ?p ?o }";
		this.params="GROUP BY ?p ORDER BY DESC(?pCount) LIMIT "+limit;
		ResultSet results=this.runQuery();
		virtuosoBridgeTools.somewhatPrettyPrint(vars, results);
	}

	/*
	 * Run Query against virtuoso graph
	 */
	private ResultSet runQuery(){
		String query = prefix+" "+select+" "+where+" "+params;
		//System.out.println(query);
		Query sparql = QueryFactory.create(query);

		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, set);

		ResultSet results = vqe.execSelect();
		return results;

	}

	public void testingOnTestGraph(){

		Node foo1 = NodeFactory.createURI("http://test/term/foo1");
		Node bar1 = NodeFactory.createURI("http://test/relation/être");
		Node baz1 = NodeFactory.createURI("http://test/term/baz1");

		Node foo2 = NodeFactory.createURI("http://test/term/foo2");
		Node bar2 = NodeFactory.createURI("http://test/relation/bar2");
		Node baz2 = NodeFactory.createURI("http://test/term/baz2");

		Node foo3 = NodeFactory.createURI("http://test/term/foo3");
		Node bar3 = NodeFactory.createURI("http://test/relation/bar3");
		Node baz3 = NodeFactory.createURI("http://test/term/baz3");

		set.add(new Triple(foo1, bar1, baz1));
		set.add(new Triple(foo1, bar2, baz1));
		set.add(new Triple(foo2, bar1, baz2));
		set.add(new Triple(foo3, bar2, baz3));
		set.add(new Triple(foo2, bar2, baz1));
		set.add(new Triple(foo1,bar3,baz1));
		set.add(new Triple(foo1,bar3,baz2));
		set.add(new Triple(foo1,bar3,baz3));
		System.out.println("graph.getCount() = " + set.getCount());

	}

	/*
	 * Generate the relationProfile of a category and add it to relationGraph for quicker future access.
	 */
	
	public void generateCatRelationProfileGraph(String catURI){
		HashMap<String, Double> relationProfile =this.calcRelProfileFromCat(catURI);
		Iterator<Entry<String, Double>> iter =relationProfile.entrySet().iterator();

		catURI=catURI.replace("term:", "cat:");
		
		while(iter.hasNext()){
			String query=prefix+" INSERT INTO GRAPH <http://wikiDataRelProfile> {";

			Entry<String,Double>currentEntry=iter.next();
			String currentRelation=currentEntry.getKey();
			Double currentValue=currentEntry.getValue();
			String tripleToAdd=catURI+" <"+currentRelation + "> " + currentValue;
			query+=tripleToAdd+". ";
			query+="}";
			
			//System.out.println(query);

			VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(query, set);
			vur.exec(); 

		}

	}

	
	/*
	 * Generate relationGraph for a term for quicker access.
	 * 
	 */
	private void generateSingleTermRelationProfileGraph(String uri){

		HashMap<String, Double> relationProfile = this.findEntityRelationProfile(uri);
		Iterator<Entry<String, Double>> iter =relationProfile.entrySet().iterator();

		while(iter.hasNext()){
			String query=prefix+" INSERT INTO GRAPH <http://wikiDataRelProfile> {";

			Entry<String,Double>currentEntry=iter.next();
			String currentRelation=currentEntry.getKey();
			Double currentValue=currentEntry.getValue();
			String tripleToAdd=uri+" <"+currentRelation + "> " + currentValue;
			query+=tripleToAdd+". ";
			query+="}";
			System.out.println(query);

			VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(query, set);
			vur.exec(); 

		}

	}

	private LinkedList<String> fetchAllDistinctArg1Terms(){

		LinkedList<String> resultList = new LinkedList<String>();
		ArrayList<String> vars= new ArrayList<String>();
		vars.add("a");
		this.select="SELECT DISTINCT ?a ";
		this.where="WHERE { ?a ?b ?c }";
		this.params="";

		ResultSet results=this.runQuery();
		//somewhatPrettyPrint(vars, results);
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String entity="<"+result.get("a").toString()+">";
			resultList.add(entity);

		}

		return resultList;
	}

	
	/*
	 *  Generate the relation profile of all terms in database and add them to the relationGraph for quicker access.
	 */
	public void generateAllSingleTermRelationProfiles(){

		clearRelProfileGraph();  

		LinkedList<String> termList=this.fetchAllDistinctArg1Terms();
		Iterator<String> iter=termList.iterator();
		while (iter.hasNext()){
			String currentUri=iter.next();
			this.generateSingleTermRelationProfileGraph(currentUri);
		}


	}

	/*
	 * Clear the relationGraph 
	 */
	public void clearRelProfileGraph() {
		String str = "CLEAR GRAPH <http://wikiDataRelProfile>";
		VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(str, set);
		vur.exec();
	}

}


