package virtuosoBridge;

import java.util.Iterator;
import java.util.Set;

public class TestingStuffSimilarity extends SimilarityMeasure {

	public TestingStuffSimilarity() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Double calcSimilarity(RelationProfile profile1, RelationProfile profile2 ,RelationProfile normalDistribution) {
		// TODO Auto-generated method stub
		//Need to try to get mostRelRelation and leastRel
		//Need to trysubstractin etre
	
		
		
		
		
		RelationProfile diffProfile1=profile1.computeDiffBetween2Profile(normalDistribution);
		RelationProfile diffProfile2=profile2.computeDiffBetween2Profile(normalDistribution);
		
		Set<String> terms=virtuosoBridgeTools.getIntersectionFrom2HashMap(profile1.getProfile(), profile2.getProfile());
		
		
		RelationProfile profile1ToCompare=profile1;
		RelationProfile profile2ToCompare=profile2;
		
		profile1.removeRelationFromProfile("relation:être");
		profile2.removeRelationFromProfile("relation:être");
		
		profile1ToCompare.smooth(terms, 0.0);
		profile2ToCompare.smooth(terms, 0.0);


		Iterator<String> iter=terms.iterator();
		Double numerator=0.0;
		Double aSquared=0.0;
		Double bSquared=0.0;
		while (iter.hasNext()){
			String currentEntry=iter.next();
			Double val1=profile1ToCompare.getProfile().get(currentEntry);
			Double val2=profile2ToCompare.getProfile().get(currentEntry);
			numerator+=(val1*val2);
			aSquared+=val1*val1;
			bSquared+=val2*val2;
		}
		Double denominator= Math.sqrt(aSquared)*Math.sqrt(bSquared);

		Double cosSimilarity=numerator/denominator;

		return cosSimilarity;
	}

}
