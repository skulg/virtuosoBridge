package virtuosoBridge;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;

import virtuoso.jena.driver.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
	private String prefix="PREFIX relation: <http://test/relation/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX term: <http://test/term/>";
	private String select = "";
	private String where ="";
	private String params="";



	public WikiQuerier(String user,String pass, String server , String graph){
		this.user=user;
		this.pass=pass;
		this.server=server;
		set=new VirtGraph(graph,"jdbc:virtuoso://"+server+"/charset=UTF-8/log_enable=2", user, pass);

	}

	/*
	 * Return Whole DataSet
	 * 
	 */
	public void selectAll(){
		ArrayList<String> vars= new ArrayList<String>();
		vars.add("s");
		vars.add("p");
		vars.add("o");

		this.select="SELECT *";
		this.where="WHERE {?s ?p ?o }";

		ResultSet results=this.runQuery();
		somewhatPrettyPrint(vars, results);

	}

	/*
	 * Return the relation profile of Entity corresponding to URI
	 */
	public HashMap<String, Double> findEntityRelationProfile(String uri){
		int normalFactor=1;

		this.select="SELECT (count(?p) AS ?totalCount)";
		this.where="WHERE {"+uri +" ?p ?o }";
		this.params="";
		ResultSet tempResult = this.runQuery();

		if(tempResult.hasNext()){
			QuerySolution result = tempResult.nextSolution();
			RDFNode tCount= result.get("totalCount");
			normalFactor=Integer.valueOf(entityCleaner(tCount.toString()));
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
			Double prob=Double.valueOf(entityCleaner(result.get("pCount").toString()));
			relationMap.put(relation,prob);

		}
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
	 * Sum 2 relation profile and Normalize
	 */

	public HashMap<String, Double> sum2RelationProfileAndNormalize(HashMap<String, Double> map1 , HashMap<String, Double> map2){
		HashMap<String, Double> resultMap=map1;

		Iterator<Entry<String, Double>> iter = map2.entrySet().iterator();

		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();
			String key = currentEntry.getKey();
			Double val1 = currentEntry.getValue();
			if (resultMap.containsKey(key)){
				Double val2 = resultMap.get(key);
				resultMap.put(key, (val1+val2));
			}else{
				resultMap.put(key, val1);
			}

		}

		iter=resultMap.entrySet().iterator();

		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();
			String key = currentEntry.getKey();
			Double val1 = currentEntry.getValue();
			resultMap.put(key, val1/2);

		}

		return resultMap;
	}


	/*
	 *  Fetch relations profiles of List and sum them up and normalize on the way
	 */

	private HashMap<String, Double> fetchSumNormalizeRelationsProfiles(LinkedList<String> list){
		HashMap<String, Double> resultMap = new HashMap<String,Double>();

		Iterator<String> iter =list.iterator();

		//FETCH first occurence out of loop to initialize resultMap
		if (iter.hasNext()){
			String currentUri="<"+iter.next()+">";
			HashMap<String, Double> currentMap =this.findEntityRelationProfile(currentUri);
			resultMap=currentMap;
			resultMap=sum2RelationProfileAndNormalize(resultMap, currentMap);


		}
		while (iter.hasNext()){
			String currentUri="<"+iter.next()+">";
			//System.out.println("");

			//System.out.println("Treating:" + currentUri);
			HashMap<String, Double> currentMap =this.findEntityRelationProfile(currentUri);

			//System.out.println("Before MAP !!!!!!!");
			//this.printSortedRelationProfile(resultMap);
			//System.out.println("ADDING TOMAP !!!!!!!");

			//this.printSortedRelationProfile(currentMap);


			resultMap=sum2RelationProfileAndNormalize(resultMap, currentMap);
			//System.out.println("RESULT MAP !!!!!!!");

			//this.printSortedRelationProfile(resultMap);


		}


		return resultMap;
	}

	/*
	 * Compare 2 relationProfile for ordering
	 */

	public HashMap<String, Double> compare2RelationProfile(HashMap<String, Double> set1 ,HashMap<String, Double> set2){

		Iterator<Entry<String, Double>> iter=set1.entrySet().iterator();
		HashMap<String, Double> resultMap= new HashMap<String, Double>();
		while (iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String key=currentEntry.getKey();
			Double val1=currentEntry.getValue();
			Double val2=(double) 0;
			if(set2.containsKey(key)){
				val2=set2.get(key);
			}
			Double resultVal=val2-val1;
			resultMap.put(key,resultVal);

		}

		return resultMap;

	}


	public void printSortedRelationProfile(HashMap<String, Double> map){
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();

		resultList=this.hashMapToSortedLinkedList(map);

		for(Entry<String,Double> e:resultList){
			System.out.println(e.getKey()+" "+e.getValue());
		}
	}

	private LinkedList<Entry<String,Double>> hashMapToSortedLinkedList(HashMap<String, Double> map){

		Iterator<Entry<String, Double>> iter=map.entrySet().iterator();
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();
		while (iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String key=currentEntry.getKey();
			Double val=currentEntry.getValue();
			resultList.add(new AbstractMap.SimpleEntry<String, Double>(key,val));
		}
		Collections.sort(resultList, new MyListComparator());
		return resultList;



	}
	class MyListComparator implements Comparator<Entry<String,Double>>{

		@Override
		public int compare(Entry<String,Double> e1, Entry<String,Double> e2) {
			if(e1.getValue() < e2.getValue()){
				return 1;
			} else if(e1.getValue() > e2.getValue()) {
				return -1;
			}else{
				return 0;
			}
		}
	}
	/*
	 *  Generate normalRelation profile
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

			Double prob=Double.valueOf(entityCleaner(result.get("pCount").toString()));
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
	 *  Return most frequent Relations
	 */

	public void selectMostFreqRelations(int limit){
		ArrayList<String> vars= new ArrayList<String>();	
		vars.add("p");
		vars.add("pCount");
		this.select="SELECT ?p (count(?p) AS ?pCount)";
		this.where="WHERE {?s ?p ?o }";
		this.params="GROUP BY ?p ORDER BY DESC(?pCount) LIMIT "+limit;
		ResultSet results=this.runQuery();
		somewhatPrettyPrint(vars, results);
	}

	/*
	 *  Pretty up a QuerySolution and print it accordind to list of vars
	 */
	private void somewhatPrettyPrint(ArrayList<String> vars , ResultSet results){
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			Iterator<String> iter=vars.iterator();
			String prettyString="  |||  ";
			RDFNode graph_name = result.get("graph");
			//prettyString+=graph_name+ "{ ";
			while (iter.hasNext()){
				String currentElem=iter.next();
				prettyString=prettyString+entityCleaner(result.get(currentElem).toString())+"  |||  ";


			}			
			System.out.println(prettyString);
		}

	}

	/*
	 * Clean URI of prefix and datatype before printing
	 */
	private String entityCleaner(String s){
		String result="";
		String[] temp=s.split("\\^\\^");
		String[] parts=temp[0].split("/");
		result=parts[parts.length-1];
		result=result.replace("_", " ");
		return result;
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

	public void printHashMap(HashMap<String, Double> map){

		System.out.println("----------------");
		System.out.println("Printing HASHMAP");
		System.out.println("----------------");

		for (String name: map.keySet()){

			String key =name.toString();
			String value = map.get(name).toString();  
			System.out.println(key + " " + value);  


		} 
	}


	private void testingOnTestGraph(){

		

		Node foo1 = NodeFactory.createURI("http://example.org/#foo1");
		Node bar1 = NodeFactory.createURI("http://example.org/#bar1");
		Node baz1 = NodeFactory.createURI("http://example.org/#baz1");

		Node foo2 = NodeFactory.createURI("http://example.org/#foo2");
		Node bar2 = NodeFactory.createURI("http://example.org/#bar2");
		Node baz2 = NodeFactory.createURI("http://example.org/#baz2");

		Node foo3 = NodeFactory.createURI("http://example.org/#foo3");
		Node bar3 = NodeFactory.createURI("http://example.org/#bar3");
		Node baz3 = NodeFactory.createURI("http://example.org/#baz3");
		
		set.add(new Triple(foo1, bar1, baz1));
		set.add(new Triple(foo1, bar2, baz1));
		set.add(new Triple(foo2, bar1, baz2));
		set.add(new Triple(foo3, bar2, baz3));
		set.add(new Triple(foo2, bar2, baz1));
		System.out.println("graph.getCount() = " + set.getCount());
		
		
		
		

	}

}


