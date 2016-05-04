package virtuosoBridge;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.store.Hash;


public class virtuosoBridgeTools {

	/*
	 *Inner class for comparing 2 entry in a relation Profile. Used for ordering.
	 */
	static class MyListComparator implements Comparator<Entry<String,Double>>{
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
	 * Get the union of the keySet of 2 hashMap
	 */
	public static Set<String> getAllKeysFrom2HashMap(HashMap<String,Double> map1,HashMap<String,Double> map2) {

		Set<String>  resultSet= new HashSet<String>();
		resultSet.addAll(map1.keySet());
		resultSet.addAll(map2.keySet());
		return resultSet;
	}


	/*
	 * 
	 * Get the intersection of 2 HashMap
	 * 
	 */
	public static Set<String> getIntersectionFrom2HashMap(HashMap<String, Double> map1 , HashMap<String, Double> map2){
		Set<String>  resultSet= new HashSet<String>();

		Iterator<String> iter = map1.keySet().iterator();

		while (iter.hasNext()){
			String currentTerm = iter.next();

			if (map2.containsKey(currentTerm)){
				resultSet.add(currentTerm);
			}

		}



		return resultSet;
	}


	/*
	 *  Print a HashMap 
	 */
	static public void printHashMap(HashMap<String, Double> map){

		System.out.println("----------------");
		System.out.println("Printing HASHMAP");
		System.out.println("----------------");

		for (String name: map.keySet()){

			String key =name.toString();
			String value = map.get(name).toString();  
			System.out.println(key + " " + value);  
		} 
	}


	/*
	 * Take a HashMap as input and produce a sorted LinkedList as output
	 */
	static public LinkedList<Entry<String,Double>> hashMapToSortedLinkedList(HashMap<String, Double> map){

		Iterator<Entry<String, Double>> iter=map.entrySet().iterator();
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();
		while (iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String key=currentEntry.getKey();
			Double val=currentEntry.getValue();
			resultList.add(new AbstractMap.SimpleEntry<String, Double>(key,val));
		}
		Collections.sort(resultList, new virtuosoBridgeTools.MyListComparator());
		return resultList;
	}

	/*
	 * Clean URI of prefix and datatype before printing
	 */
	static String entityCleaner(String s){
		String result="";
		String[] temp=s.split("\\^\\^");
		String[] parts=temp[0].split("/");
		result=parts[parts.length-1];
		result=result.replace("_", " ");
		return result;
	}

	/*
	 *  Pretty up a QuerySolution and print it according to list of vars
	 */
	static void somewhatPrettyPrint(ArrayList<String> vars , ResultSet results){
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			Iterator<String> iter=vars.iterator();
			String prettyString="  |||  ";
			RDFNode graph_name = result.get("graph");
			while (iter.hasNext()){
				String currentElem=iter.next();
				prettyString=prettyString+entityCleaner(result.get(currentElem).toString())+"  |||  ";
			}			
			System.out.println(prettyString);
		}

	}


	//Check the difference between 2 relationVectors
	static public HashMap<String, Double> compare2RelationProfile(HashMap<String, Double> set1 ,HashMap<String, Double> set2){
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

	static String prefixedTermToFullURI(String prefix){
		String resultString="";

		if(prefix.contains("term:")){
			resultString=prefix.replaceFirst("term:", "http://test/term/");
		}else if(prefix.contains("relation:")){
			resultString=prefix.replaceFirst("relation:", "http://test/relation/");
		}


		return resultString;
	}


	/*
	 * Take a resultSet as input and output a HashMap corresponding to RelationProfile
	 */
	static void resultSetToRelationMap(HashMap<String, Double> relationMap, ResultSet results) {
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
			String relation=result.get("p").toString();
			Double prob=Double.valueOf(entityCleaner(result.get("o").toString()));
			relationMap.put(relation,prob);

		}
	}
	
	
	
	
	static void removeSet2From1AndClean(LinkedList<String> set1,HashSet<String> set2){
		
		Iterator<String> iter=set2.iterator();
		
		
		while(iter.hasNext()){
			set1.remove("<"+iter.next()+">");
		}
		
	}


	public static LinkedList<String> getRandomSubListFromList(LinkedList<String> termList, int nbToFetch) {
		LinkedList<String> resultList=new LinkedList<String>();
		Collections.shuffle(termList);
		int nbAdded=0;
		while(nbAdded<nbToFetch){
			resultList.add(termList.pop());
			nbAdded++;
		}
		// TODO Auto-generated method stub
		return resultList;
	}
	

}
