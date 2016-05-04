package virtuosoBridge;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.base.Sys;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;
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
	private HashMap<String , RelationProfile> cachedCatRelProfile;

	public WikiQuerier(String user,String pass, String server , String graph){
		this.user=user;
		this.pass=pass;
		this.server=server;
		this.graph=graph;
		set=new VirtGraph(this.graph,"jdbc:virtuoso://"+this.server+"/charset=UTF-8/log_enable=2", this.user, this.pass);
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
	public RelationProfile findEntityRelationProfile2(String uri){

		//System.out.println("Generating profile for "+ uri);

		int normalFactor = findTotalCountOfTerm(uri);

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
		RelationProfile resultProfile=new RelationProfile(relationMap);
		resultProfile.setName(uri);
		return resultProfile;	
	}

	private int findTotalCountOfTerm(String uri) {
		int normalFactor=0;

		this.select="SELECT (count(?p) AS ?totalCount)";
		this.where="WHERE {"+uri +" ?p ?o }";
		this.params="";
		ResultSet tempResult = this.runQuery();

		if(tempResult.hasNext()){
			QuerySolution result = tempResult.nextSolution();
			RDFNode tCount= result.get("totalCount");
			normalFactor=Integer.valueOf(virtuosoBridgeTools.entityCleaner(tCount.toString()));
		}
		return normalFactor;
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
	public RelationProfile calcRelProfileFromCat(String uri){

		LinkedList<String> list= new LinkedList<String>();

		//System.out.println("Fetching entities belonging to :"+uri);
		list=fetchAllEntitiesBelongingToCat(uri);


		System.out.println("Calculating Relations Profiles from List");
		RelationProfile resultProfile=this.fetchSumNormalizeRelationsProfiles(list);
		resultProfile.setName(uri);
		return resultProfile;
	}



	//Fetch relation graph belonging to URI category instead of calculating from terms.
	public RelationProfile fetchRelProfileFromGraph(String uri){

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
		RelationProfile resultProfile=new RelationProfile(relationMap);
		resultProfile.setName(uri);
		return 	resultProfile;

	}

	//Fetch cached relation graph 
	public RelationProfile fetchRelProfileFromHashMap(String uri){
		if (this.cachedCatRelProfile==null){
			System.out.println("Relation profiles for categories not present in cache. Fetching please be patient.");
			this.cachedCatRelProfile=this.loadCatsRelationProfileFromGraph();
			System.out.println("Fetching Done.");

		}
		RelationProfile returnProfile=this.cachedCatRelProfile.get(uri);


		return returnProfile;


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

	private RelationProfile fetchSumNormalizeRelationsProfiles(LinkedList<String> list){
		RelationProfile resultProfile = new RelationProfile();

		Iterator<String> iter =list.iterator();

		while (iter.hasNext()){
			String currentUri=iter.next();
			if(!(currentUri.charAt(0)=='<')){
				currentUri="<"+currentUri+">";
			}
			RelationProfile currentProfile =this.findEntityRelationProfile2(currentUri);		
			resultProfile.sumOtherProfile(currentProfile);

		}

		resultProfile.normalize();

		return resultProfile;
	}


	public RelationProfile generateNormalRelationProfile(){

		System.out.println("Generating Normal Relation Profile");

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
		RelationProfile resultProfile=new RelationProfile(normalRelationMap);	
		resultProfile.setName("NormalProfile");

		return resultProfile;
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


	public void generateListCatRelationProfileGraph(LinkedList<String> list){

		this.clearRelProfileGraph();
		int totalCount =list.size();
		int count=0;
		Iterator<String> iter=list.iterator();

		while(iter.hasNext()){
			String cat=iter.next();
			System.out.println("");
			System.out.println("Generating graph for "+cat);
			System.out.println(""+count+"/"+totalCount);
			generateCatRelationProfileGraph(cat);
			count++;

		}



	}

	/*
	 * Generate the relationProfile of a category and add it to relationGraph for quicker future access.
	 */

	public void generateCatRelationProfileGraph(String catURI){
		RelationProfile relationProfile =this.calcRelProfileFromCat(catURI);
		Iterator<Entry<String, Double>> iter =relationProfile.getProfile().entrySet().iterator();

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
	public LinkedList<String> fetchDistinctArg1TermsInFreqOrder(int nbToFetch){


		System.out.println("Fetching "+nbToFetch +" most frequent terms");

		LinkedList<String> resultList = new LinkedList<String>();
		ArrayList<String> vars= new ArrayList<String>();
		vars.add("a");


		this.select="SELECT * ";
		this.where="WHERE {SELECT DISTINCT ?a WHERE {?a ?b ?c} GROUP BY ?a ORDER BY DESC (count (?a)) }";
		this.params="LIMIT "+nbToFetch;



		ResultSet results=this.runQuery();
		//somewhatPrettyPrint(vars, results);
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String entity="<"+result.get("a").toString()+">";
			resultList.add(entity);

		}

		return resultList;
	}




	public HashMap<String,Double> findTermSimilarityToCats(String term,LinkedList<String> cats ,SimilarityMeasure measure ){

		Iterator<String> iter=cats.iterator();
		RelationProfile termRelProfile=findEntityRelationProfile2(term);
		//RelationProfile normalProfile=generateNormalRelationProfile();
		RelationProfile normalProfile=null; //TEMP TO SPEED UP SINCE MYMeasure Doesnt need normalProfile
		HashMap<String,Double> similarityResults= new HashMap<String,Double>();
		String currentCat;
		RelationProfile catRelProfile;

		while(iter.hasNext()){
			currentCat=iter.next();
			currentCat=currentCat.replace("term:", "cat:");
			catRelProfile=this.fetchRelProfileFromHashMap(currentCat);

			Double currentSimilarity=termRelProfile.findSimilarityLevel(catRelProfile,normalProfile,measure);
			similarityResults.put(currentCat, currentSimilarity);

		}
		return similarityResults;

	}

	public void assignAndAddToGraphCatSimilarity(String term ,LinkedList<String> cats , SimilarityMeasure measure , int nbOfCatToAssignToTerm ){
		Entry<String,Double> catAssigned=null;
		HashMap<String, Double> similarityResults=findTermSimilarityToCats(term, cats, measure);

		Iterator <Entry<String,Double>> iter = virtuosoBridgeTools.hashMapToSortedLinkedList(similarityResults).iterator();
		int count=0;

		while(iter.hasNext() & count< nbOfCatToAssignToTerm){		
			catAssigned=iter.next();
			this.addCatTermSimilarityToGraph(term, catAssigned);
			count++;
		}


	}


	public Entry<String,Double> assignCatToTerm(String term , LinkedList<String> cats , SimilarityMeasure measure){
		Entry<String,Double> catAssigned=null;
		HashMap<String, Double> similarityResults=findTermSimilarityToCats(term, cats, measure);

		Iterator <Entry<String,Double>> iter = virtuosoBridgeTools.hashMapToSortedLinkedList(similarityResults).iterator();

		if (iter.hasNext()){
			catAssigned=iter.next();
		}


		return catAssigned;
	}


	public void assignAllTermsACat(LinkedList<String> cats,SimilarityMeasure measure){
		System.out.println("Clearing old Assignement Graph");
		this.clearAssignementGraph();

		System.out.println("Fetching All terms to classify");

		LinkedList<String> list=this.fetchAllDistinctArg1Terms();

		System.out.println("Done");
		System.out.println("");


		this.assignAllTermsInListACat(list, cats, measure);



	}

	public void resumeTermAssignementLinkedList (LinkedList<String> termsToClassify,LinkedList<String> cats,SimilarityMeasure measure){
		boolean foundResumingSpot=false;

		while (!foundResumingSpot &termsToClassify.size()>0 ){
			if (this.isCatAssigned(termsToClassify.getFirst())){
				System.out.println(termsToClassify.removeFirst() + " was already classified skipping");
			}else{
				foundResumingSpot=true;
			}
		}

		assignAllTermsInListACat(termsToClassify, cats, measure);

	}


	public void resumeTermCatSimilarityLinkedList (LinkedList<String> termsToClassify,LinkedList<String> cats,SimilarityMeasure measure ,int nbOfCatsToAssignPerTerms){
		boolean foundResumingSpot=false;

		while (!foundResumingSpot &termsToClassify.size()>0 ){
			if (this.isCatSimilarityInGraph(termsToClassify.getFirst())){
				System.out.println(termsToClassify.removeFirst() + " was already classified skipping");
			}else{
				foundResumingSpot=true;
			}
		}

		assignAllTermsInListACatSimilarity(termsToClassify, cats, measure,nbOfCatsToAssignPerTerms);

	}



	public void assignAllTermsInListACat(LinkedList<String> termsToClassify,LinkedList<String> cats,SimilarityMeasure measure){
		System.out.println("Starting Classification");

		final long startTime = System.nanoTime();
		int totalItemCount=termsToClassify.size();
		int nbItemsProcessed=0;
		long duration;
		Long eta;
		Entry<String,Double> assignedCatAndSimilarity;
		Iterator<String> iter=termsToClassify.iterator();
		String currentTerm;
		while(iter.hasNext()){
			currentTerm =iter.next();
			duration = (System.nanoTime() - startTime)/1000000000;
			System.out.println("Treating "+currentTerm + " "+nbItemsProcessed+"/"+totalItemCount);



			assignedCatAndSimilarity=this.assignCatToTerm(currentTerm, cats, measure);

			addCatAssignementToGraph(currentTerm,assignedCatAndSimilarity);
			nbItemsProcessed++;
			eta=totalItemCount/nbItemsProcessed*duration;
			System.out.println(""+nbItemsProcessed+"/"+totalItemCount + " time:" + duration + " eta:"+eta);

			System.out.println("");
		}
		System.out.println("Done");
		System.out.println("");

	}


	public void assignAllTermsInListACatSimilarity(LinkedList<String> termsToClassify,LinkedList<String> cats,SimilarityMeasure measure , int nbOfCatsToAssignPerTerms){

		System.out.println("");
		System.out.println("Starting Cat Similarity calculation");
		final long startTime = System.nanoTime();
		int totalItemCount=termsToClassify.size();
		int nbItemsProcessed=0;
		long duration;
		Long eta;


		Iterator<String> iter=termsToClassify.iterator();
		String currentTerm;

		while(iter.hasNext()){
			currentTerm =iter.next();
			duration = (System.nanoTime() - startTime)/1000000000;
			System.out.println("");
			System.out.println("========================");
			System.out.println("Treating "+currentTerm);



			this.assignAndAddToGraphCatSimilarity(currentTerm,cats , measure, nbOfCatsToAssignPerTerms);

			nbItemsProcessed++;
			eta=totalItemCount/nbItemsProcessed*duration;
			System.out.println(""+nbItemsProcessed+"/"+totalItemCount + " time:" + duration + " eta:"+eta);
			System.out.println("");
		}
		System.out.println("Done");
		System.out.println("");

	}



	public void addCatAssignementToGraph(String termClassified , Entry<String,Double> assignementEntry){


		String query=prefix+" INSERT INTO GRAPH <http://wikiDataCatAssignement> {";


		String cat = assignementEntry.getKey();
		Double similarityLevel=assignementEntry.getValue();

		String tripleToAdd=termClassified+" "+cat + " " + similarityLevel;
		query+=tripleToAdd+". ";
		query+="}";

		System.out.println(tripleToAdd);



		VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(query, set);
		vur.exec(); 

	}

	public void addCatTermSimilarityToGraph(String termClassified, Entry<String,Double> assignementEntry){
		String query=prefix+" INSERT INTO GRAPH <http://wikiDataCatSimilarity> {";


		String cat = assignementEntry.getKey();
		Double similarityLevel=assignementEntry.getValue();

		String tripleToAdd=termClassified+" "+cat + " " + similarityLevel;
		query+=tripleToAdd+". ";
		query+="}";

		System.out.println(tripleToAdd);


		VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(query, set);
		vur.exec(); 

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

	/*
	 * Clear the AssignementGraph 
	 */
	public void clearAssignementGraph() {
		String str = "CLEAR GRAPH <http://wikiDataCatAssignement>";
		VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(str, set);
		vur.exec();
	}

	public void clearCatSimilarityGraph() {
		String str = "CLEAR GRAPH <http://wikiDataCatSimilarity>";
		VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(str, set);
		vur.exec();
	}


	/*
	 * Create terms set for terms not in cats list
	 */
	public LinkedList<String> createOthersCatTermSet(LinkedList<String> cats){


		LinkedList<String> resultList =this.fetchAllDistinctArg1Terms(); //Fetch All terms

		System.out.println(resultList.size());

		Iterator<String> iter= cats.iterator();

		HashSet<String> cummulativeList=new HashSet<String>();
		while(iter.hasNext()){
			String currentCat=iter.next();
			LinkedList<String> currentList=this.fetchAllEntitiesBelongingToCat(currentCat);
			cummulativeList.addAll(currentList);
		}

		System.out.println(cummulativeList.size());

		virtuosoBridgeTools.removeSet2From1AndClean(resultList, cummulativeList); //Remove term assigned to cat from global list

		System.out.println(resultList.size());
		return resultList;

	}

	/*
	 * Generate otherCatRelProfile
	 */

	public RelationProfile genOtherCatRelProfile(LinkedList<String> cats){

		LinkedList<String> otherTerms =this.createOthersCatTermSet(cats);

		RelationProfile resultProfile=this.fetchSumNormalizeRelationsProfiles(otherTerms);
		resultProfile.setName("Other");
		return resultProfile;

	}

	/*
	 * Generate otherCatRelProfile
	 * Faster implementation when number of terms in other cats is large
	 */

	public RelationProfile genOtherCatRelProfilev2(LinkedList<String> cats){

		Iterator<String> iter = cats.iterator();
		RelationProfile catNormalRelProfile=new RelationProfile();
		while(iter.hasNext()){
			String currentCat=iter.next();
			RelationProfile currentProfile=this.fetchRelProfileFromGraph(currentCat);
			catNormalRelProfile.sumOtherProfile(currentProfile);

		}
		catNormalRelProfile.normalize();
		RelationProfile normalProfile = this.generateNormalRelationProfile();

		RelationProfile resultProfile=new RelationProfile();

		catNormalRelProfile.smooth2Profiles(normalProfile, 0.0);
		Iterator<Entry<String,Double>> iter2=catNormalRelProfile.getProfile().entrySet().iterator();

		while(iter2.hasNext()){
			Entry<String,Double> currentEntry = iter2.next();
			String currentKey=currentEntry.getKey();
			Double currentValue=currentEntry.getValue();
			Double normalValue=normalProfile.getProfile().get(currentKey);
			Double resultValue=Math.abs(normalValue-currentValue);
			resultProfile.getProfile().put(currentKey, resultValue);
		}
		resultProfile.normalize();

		iter2=resultProfile.getProfile().entrySet().iterator();

		while(iter2.hasNext()){

			String query=prefix+" INSERT INTO GRAPH <http://wikiDataRelProfile> {";

			Entry<String,Double>currentEntry=iter2.next();
			String currentRelation=currentEntry.getKey();
			Double currentValue=currentEntry.getValue();
			String tripleToAdd="cat:OTHERcATS <"+currentRelation + "> " + currentValue;
			query+=tripleToAdd+". ";
			query+="}";

			//System.out.println(query);

			VirtuosoUpdateRequest vur  = VirtuosoUpdateFactory.create(query, set);
			vur.exec(); 

		}

		resultProfile.setName("Other");
		return resultProfile;


	}

	public LinkedList<String> manualCatsList(){
		LinkedList<String> catToCalcRelProfile= new LinkedList<String>();
		catToCalcRelProfile.add("term:une_ville");
		catToCalcRelProfile.add("term:un_acteur");
		catToCalcRelProfile.add("term:un_village");
		catToCalcRelProfile.add("term:un_film_américain");
		catToCalcRelProfile.add("term:un_écrivain");
		catToCalcRelProfile.add("term:un_acteur_américain");
		catToCalcRelProfile.add("term:un_joueur_professionnel_de_hockey");
		catToCalcRelProfile.add("term:un_journaliste");
		catToCalcRelProfile.add("term:un_peintre");
		catToCalcRelProfile.add("term:un_film");
		catToCalcRelProfile.add("term:un_chanteur");
		catToCalcRelProfile.add("term:une_chanson");
		catToCalcRelProfile.add("term:un_joueur");
		catToCalcRelProfile.add("term:un_musicien");
		catToCalcRelProfile.add("term:un_groupe_de_musique");
		catToCalcRelProfile.add("term:un_philosophe");		
		catToCalcRelProfile.add("term:un_professeur");
		catToCalcRelProfile.add("term:une_chanteuse");

		catToCalcRelProfile.add("term:un_président");


		return catToCalcRelProfile;
	}


	public LinkedList<String> findTopCats(int limit){
		LinkedList<String> resultList = new LinkedList<String>();

		ArrayList<String> vars= new ArrayList<String>();	
		vars.add("p");
		vars.add("pCount");

		this.select="SELECT ?p (count(?p) AS ?pCount)";
		this.where="WHERE {?s relation:être ?p. FILTER ( regex (str(?p), '/un[e]*_') ) }";
		this.params="GROUP BY ?p ORDER BY DESC(?pCount) LIMIT "+limit;
		ResultSet results=this.runQuery();
		//virtuosoBridgeTools.somewhatPrettyPrint(vars, results);
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String category="<"+result.get("p").toString()+">";
			resultList.add(category);
		}

		return resultList;

	}

	/*
	 * Load all Relation Profiles describing categories into an HashMap to speed up 
	 * multiples queries
	 */

	public HashMap<String,RelationProfile> loadCatsRelationProfileFromGraph(){

		HashMap<String, RelationProfile> resultMap= new HashMap<String,RelationProfile>();

		ArrayList<String> vars= new ArrayList<String>();	
		vars.add("?cat");
		vars.add("?rel");
		vars.add("?prob");

		this.select="SELECT ?cat ?rel ?prob FROM <http://wikiDataRelProfile>";
		this.where="WHERE {graph <http://wikiDataRelProfile> {?cat ?rel ?prob}}";
		this.params="ORDER BY ?cat";
		ResultSet results=this.runQuery();
		//virtuosoBridgeTools.somewhatPrettyPrint(vars, results);


		String currentProfileCat="";

		HashMap<String, Double> currentProfile =new HashMap<String,Double>();


		String currentCat;
		String currentRel;
		Double currentProb;

		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			currentCat= result.get("cat").toString();
			currentRel=result.get("rel").toString();
			currentProb=Double.valueOf(virtuosoBridgeTools.entityCleaner(result.get("prob").toString()));

			if(!(currentProfileCat.equals(currentCat))){

				if(!(currentProfileCat=="")){
					RelationProfile profileToAdd =new RelationProfile(currentProfile); 
					resultMap.put("<"+currentProfileCat+">", profileToAdd);
				}
				currentProfileCat=currentCat;
				currentProfile=new HashMap<String,Double>();

			}

			currentProfile.put(currentRel, currentProb);

		}
		RelationProfile profileToAdd =new RelationProfile(currentProfile); 		
		resultMap.put("<"+currentProfileCat+">", profileToAdd);
		return resultMap;

	}


	/*
	 * Check if term as a cat assignation in graph
	 */
	public boolean isCatAssigned(String term){


		String query=prefix+" ASK FROM <http://wikiDataCatAssignement> WHERE {GRAPH <http://wikiDataCatAssignement>  {"+ term +" ?b ?c}}";
		Query sparql = QueryFactory.create(query);

		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, set);

		boolean res = vqe.execAsk();
		System.out.println("\nASK results: "+res);
		return res;
	}


	/*
	 * Check if term as a similarityLevel in graph
	 */
	public boolean isCatSimilarityInGraph(String term){


		String query=prefix+" ASK FROM <http://wikiDataCatSimilarity> WHERE {GRAPH <http://wikiDataCatSimilarity>  {"+ term +" ?b ?c}}";
		Query sparql = QueryFactory.create(query);

		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, set);

		boolean res = vqe.execAsk();
		System.out.println("\nASK results: "+res);
		return res;
	}

	/*
	 * Take the Graph similarity graph fetch the catAssignement for each term 
	 * and dump results into a seperate text files for each categories
	 */

	public void dumpCatAssignementGraphToTextFiles(){


		ArrayList<String> vars= new ArrayList<String>();	
		vars.add("?x");
		vars.add("?b");
		vars.add("?max");
		this.select="SELECT ?x ?b ?max FROM <http://wikiDataCatSimilarity>";
		this.where="WHERE {  graph <http://wikiDataCatSimilarity> {?x ?b ?max {SELECT ?x (MAX(?value) AS ?max) WHERE { ?x ?b ?value}GROUP BY ?x}}}";
		this.params="ORDER BY ?b DESC(?max)";
		ResultSet results=this.runQuery();

		//		virtuosoBridgeTools.somewhatPrettyPrint(vars, results);
		String currentCat;
		String currentTerm;
		Double currentSimilarity;
		String path="categoriesAssignements/";
		String currentFilename="testingStuff";
		File currentFile=new File(path+currentFilename+".txt");
		try {
			File allFile=new File(path+"all.txt");
			FileWriter writer=new FileWriter(currentFile);
			FileWriter allWriter=new FileWriter(allFile);
			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				currentCat= virtuosoBridgeTools.entityCleaner(result.get("b").toString());
				currentTerm= virtuosoBridgeTools.entityCleaner(result.get("x").toString());
				currentSimilarity=Double.valueOf(virtuosoBridgeTools.entityCleaner(result.get("max").toString()));

				if(!currentCat.equals(currentFilename)){

					currentFilename=currentCat;
					currentFile=new File(path+currentFilename+".txt");
					writer.close();

					writer = new FileWriter(currentFile);
				} 

				String lineToWrite=""+currentTerm+" "+currentSimilarity+System.getProperty("line.separator");;
				writer.write(lineToWrite);
				allWriter.write(currentCat+" || "+lineToWrite);
			}
			allWriter.close();
			writer.close();


		}

		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void dumpCatSimilarityGraphToTextFile(){


		ArrayList<String> vars= new ArrayList<String>();	
		vars.add("?x");
		vars.add("?b");
		vars.add("?c");
		this.select="SELECT ?x ?b ?c FROM <http://wikiDataCatSimilarity>";
		this.where="WHERE {  graph <http://wikiDataCatSimilarity> {?x ?b ?c }}";
		this.params="ORDER BY ?x DESC(?c) ";
		ResultSet results=this.runQuery();

		//		virtuosoBridgeTools.somewhatPrettyPrint(vars, results);
		String currentCat;
		String currentTerm;
		Double currentSimilarity;
		String path="categoriesAssignements/";
		String currentFilename="allSimilarities";
		File currentFile=new File(path+currentFilename+".txt");
		try {

			FileWriter writer=new FileWriter(currentFile);

			while (results.hasNext()) {
				QuerySolution result = results.nextSolution();
				currentCat= virtuosoBridgeTools.entityCleaner(result.get("b").toString());
				currentTerm= virtuosoBridgeTools.entityCleaner(result.get("x").toString());
				currentSimilarity=Double.valueOf(virtuosoBridgeTools.entityCleaner(result.get("c").toString()));


				String lineToWrite=""+currentTerm+" || "+currentCat+" || "+currentSimilarity+System.getProperty("line.separator");;
				writer.write(lineToWrite);

			}
			writer.close();


		}

		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	public void dumpCatRelProfilesHashMapToTextFile(){

		LinkedList<String> cats=this.findTopCats(1000);

		String currentCat;
		String currentRelation;
		Double currentSimilarity;
		String path="profiles/categoriesProfile/";
		String currentFilename="allCatsProfiles";
		File currentFile=new File(path+currentFilename+".txt");


		Iterator<String> iter=cats.iterator();
		RelationProfile currentProfile;
		Iterator<Entry<String,Double>> profileIter;
		Entry<String,Double> currentEntry;
		String lineToWrite;
		System.out.println("Starting catRelProfiles Dump");
		
		try {
			FileWriter writer=new FileWriter(currentFile);


			while(iter.hasNext()){

				currentCat=iter.next();
				currentProfile=this.fetchRelProfileFromHashMap(currentCat);
				
				profileIter=virtuosoBridgeTools.hashMapToSortedLinkedList(currentProfile.getProfile()).iterator();
				currentCat= virtuosoBridgeTools.entityCleaner(currentCat);
				while(profileIter.hasNext()){
					currentEntry=profileIter.next();

					currentRelation=virtuosoBridgeTools.entityCleaner(currentEntry.getKey());
					currentSimilarity=currentEntry.getValue();
					lineToWrite=""+currentCat+" || "+currentRelation+" || "+currentSimilarity+System.getProperty("line.separator");;
					writer.write(lineToWrite);

//					System.out.println("Writing line : "+lineToWrite);				
					
				}

			}

			System.out.println("Done");
			
			writer.close();
		}catch(Exception e){

			System.out.println("Error");
			
		}


	}
	public void dumpRandomRelProfilesFromListToTextFile(LinkedList<String> termList , int nbToFetch){

//		LinkedList<String> subList=virtuosoBridgeTools.getRandomSubListFromList(termList,nbToFetch);
		LinkedList<String> subList=termList;
		
		String currentTerm;
		String currentRelation;
		Double currentSimilarity;
		String path="profiles/termsProfile/";
		String currentFilename="allTermsProfiles";
		File currentFile=new File(path+currentFilename+".txt");


		Iterator<String> iter=subList.iterator();
		RelationProfile currentProfile;
		Iterator<Entry<String,Double>> profileIter;
		Entry<String,Double> currentEntry;
		String lineToWrite;
		System.out.println("Starting termRelProfiles Dump");
		
		try {
			FileWriter writer=new FileWriter(currentFile);


			while(iter.hasNext()){

				currentTerm=iter.next();
				currentProfile=this.findEntityRelationProfile2(currentTerm);
				
				profileIter=virtuosoBridgeTools.hashMapToSortedLinkedList(currentProfile.getProfile()).iterator();
				currentTerm= virtuosoBridgeTools.entityCleaner(currentTerm);
				while(profileIter.hasNext()){
					currentEntry=profileIter.next();

					currentRelation=virtuosoBridgeTools.entityCleaner(currentEntry.getKey());
					currentSimilarity=currentEntry.getValue();
					lineToWrite=""+currentTerm+" || "+currentRelation+" || "+currentSimilarity+System.getProperty("line.separator");;
					writer.write(lineToWrite);

//					System.out.println("Writing line : "+lineToWrite);				
					
				}

			}

			System.out.println("Done");
			
			writer.close();
		}catch(Exception e){

			System.out.println("Error");
			
		}
	}


}


