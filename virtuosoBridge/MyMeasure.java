package virtuosoBridge;

import java.util.Iterator;
import java.util.Set;

public class MyMeasure extends SimilarityMeasure {

	public MyMeasure() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Double calcSimilarity(RelationProfile inputProfile1, RelationProfile inputProfile2, RelationProfile normalDistribution) {

		
		RelationProfile profile1=new RelationProfile(inputProfile1);
		RelationProfile profile2=new RelationProfile(inputProfile2);
		
		profile1.smooth2Profiles(profile2, 0.0);
				
		profile1.removeRelationFromProfile("relation:être");
		profile2.removeRelationFromProfile("relation:être");
		
		Set<String> terms=profile1.getProfile().keySet();
	

		Iterator<String> iter=terms.iterator();
		Double numerator=0.0;
		Double aSquared=0.0;
		Double bSquared=0.0;
		Double val1;
		Double val2;
		String currentEntry;
		while (iter.hasNext()){
			currentEntry=iter.next();
			val1=profile1.getProfile().get(currentEntry);
			val2=profile2.getProfile().get(currentEntry);
			numerator+=(val1*val2);
			aSquared+=val1*val1;
			bSquared+=val2*val2;
		}
		Double denominator= Math.sqrt(aSquared)*Math.sqrt(bSquared);

		Double cosSimilarity=numerator/denominator;

		
		//AVOID the 0/0 case
		if(Double.isNaN(cosSimilarity)){
			cosSimilarity=0.0;
		}
		
		return cosSimilarity;
	}

}
