package virtuosoBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;


/*
 * Class that describe a relationProfile and its operations
 */
public class RelationProfile {

	private HashMap<String,Double> profile;
	private String name;


	public RelationProfile(HashMap<String,Double> relProfile) {
		this.profile=relProfile;
	}

	
	public RelationProfile() {
		this.profile=new HashMap<String,Double>();
	}


	//Normalize the relationProfile to a proper Distribution
	public void normalize(){
		//TOTEST
		
		Double normalFactor=this.getTotalProbMass();

		Iterator<Entry<String, Double>> iter=profile.entrySet().iterator();
		HashMap<String, Double> normalizedMap = new HashMap<String , Double>();
		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();
			String key = currentEntry.getKey();
			Double val1 = currentEntry.getValue();
			normalizedMap.put(key, val1/normalFactor);

		}
		
		
		this.profile= normalizedMap;
		
		
		
	}
	
	public void sumOtherProfile(RelationProfile profileToAdd){
		//TODO
		
		
		HashMap<String, Double> resultMap=this.profile;
		HashMap<String, Double> mapToAdd=profileToAdd.profile;
		
		Iterator<Entry<String, Double>> iter = mapToAdd.entrySet().iterator();

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
		this.profile=resultMap;
		
		
	}
	
	
	
	public Double getTotalProbMass(){
	
		Iterator<Entry<String, Double>> iter = this.profile.entrySet().iterator();
		Double totalMass=0.0;
		while(iter.hasNext()){
			Entry<String, Double> currentEntry = iter.next();

			totalMass+=currentEntry.getValue();
		}
		
		return totalMass;
		
	}

	public Double findSimilarityLevel(RelationProfile otherProfile,RelationProfile normalDistribution ,SimilarityMeasure measure ){
		return measure.calcSimilarity(this,otherProfile,normalDistribution);
		
	}

	public void printSortedProfile(){
		//TODO
		LinkedList<Entry<String,Double>> resultList=new LinkedList<Entry<String,Double>>();
		resultList=virtuosoBridgeTools.hashMapToSortedLinkedList(this.profile);
		for(Entry<String,Double> e:resultList){
			System.out.println(e.getKey()+" "+e.getValue());
		}
		
	}

	public void smooth2Profiles(RelationProfile otherProfile ,Double smoothingMass){
		Set<String> terms=virtuosoBridgeTools.getAllKeysFrom2HashMap(getProfile(), otherProfile.getProfile());
		this.smooth(terms, smoothingMass);
		otherProfile.smooth(terms, smoothingMass);
	}
	
	public void smooth(Set<String> terms, Double smoothingMass){
		//TODO
		
		Double totalMassAdded=0.0;
		
		Iterator<String> iter=terms.iterator();
		while (iter.hasNext()){
			String currentTerm=iter.next();
			Double currentValue=0.0;

			if(this.profile.containsKey(currentTerm)){
				currentValue=this.profile.get(currentTerm);
			}

			this.profile.put(currentTerm, currentValue+smoothingMass);
			totalMassAdded+=smoothingMass;
		}
	
	}


	public String toString(){
		String lineSeparator= System.getProperty("line.separator");		
		String result="";		
		for (String name: this.profile.keySet()){			
			String value = this.profile.get(name).toString();  
			String line=name+" "+value;
			result+=line+lineSeparator;
		} 		
		return result;
	}

	public RelationProfile computeDiffBetween2Profile(RelationProfile otherProfile){
		RelationProfile diffProfile=new RelationProfile();
		this.smooth2Profiles(otherProfile, 0.0);
		Iterator<Entry<String,Double>> iter=this.profile.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String,Double> currentEntry=iter.next();
			String currentKey=currentEntry.getKey();
			Double currentValue=currentEntry.getValue();
			Double otherValue=otherProfile.profile.get(currentKey);
			Double diff=currentValue-otherValue;
			diffProfile.profile.put(currentKey, diff);
		}
		
		
		return diffProfile;
	}
	
	public void compareWithOtherProfile(RelationProfile otherProfile){

		String lineToPrint="";
		String entitySeparator="|||";
		
		System.out.println("Generating Diff Relation Profile");
		
		RelationProfile diff=this.computeDiffBetween2Profile(otherProfile);
		
		LinkedList<Entry<String,Double>> orderList=virtuosoBridgeTools.hashMapToSortedLinkedList(diff.profile);
		
		Iterator<Entry<String,Double>> iter=orderList.iterator();
		while(iter.hasNext()){
			String currentKey=iter.next().getKey();
			
			Double currentValue=this.profile.get(currentKey);
			Double otherValue=otherProfile.profile.get(currentKey);
			Double diffValue=diff.profile.get(currentKey);
			
			lineToPrint=currentKey+entitySeparator+currentValue+entitySeparator+otherValue+entitySeparator+diffValue;
			
			System.out.println(lineToPrint);
		}
		
		
	
	}
	
	public Set<String> getMostFreqRel(int threshold){
		LinkedList<Entry<String,Double>>list =virtuosoBridgeTools.hashMapToSortedLinkedList(this.profile);
		int index=0;
		Set<String> resultSet=new LinkedHashSet<String>();
		while(index <list.size() && index<threshold){
			String relation=list.get(index).getKey();
			resultSet.add(relation);
			index++;
		}
		return resultSet;
	}
	
	public RelationProfile createSubsetProfile(Set<String> termSubSet){
		
		RelationProfile resultProfile = new RelationProfile();
		
		Iterator<String> iter = termSubSet.iterator();
		
		while(iter.hasNext()){
			String currentTerm =iter.next();
			resultProfile.profile.put(currentTerm, this.profile.get(currentTerm));
		}
		
	
		return resultProfile;
	}
	
	public void removeRelationsFromProfile(Set<String> setToRemove){
		
		Iterator<String> iter=setToRemove.iterator();
		
		while(iter.hasNext()){
			String currentRelation=iter.next();
			this.removeRelationFromProfile(currentRelation);
		}
		
	}
	
	public void removeRelationFromProfile(String relation){
		
		this.profile.remove(virtuosoBridgeTools.prefixedTermToFullURI(relation));
		
	}
	
	
	public HashMap<String,Double> getProfile(){
		return this.profile;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
	
}
