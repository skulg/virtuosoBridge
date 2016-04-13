package virtuosoBridge;

import java.util.Iterator;
import java.util.Set;

public class MyMeasure extends SimilarityMeasure {

	public MyMeasure() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Double calcSimilarity(RelationProfile profile1, RelationProfile profile2, RelationProfile normalDistribution) {

		
		profile1.smooth2Profiles(profile2, 0.0);
				
		profile1.removeRelationFromProfile("relation:être");
		profile2.removeRelationFromProfile("relation:être");
		
		Set<String> terms=virtuosoBridgeTools.getIntersectionFrom2HashMap(profile1.getProfile(), profile2.getProfile());

	

		Iterator<String> iter=terms.iterator();
		Double numerator=0.0;
		Double aSquared=0.0;
		Double bSquared=0.0;
		while (iter.hasNext()){
			String currentEntry=iter.next();
			Double val1=profile1.getProfile().get(currentEntry);
			Double val2=profile2.getProfile().get(currentEntry);
			numerator+=(val1*val2);
			aSquared+=val1*val1;
			bSquared+=val2*val2;
		}
		Double denominator= Math.sqrt(aSquared)*Math.sqrt(bSquared);

		Double cosSimilarity=numerator/denominator;

		return cosSimilarity;
	}

}
