package btree.tests;

import btree.BTree;
import btree.BusinessRecommender;

import java.io.IOException;

public class ClusterTest {
	
//	private static class Cluster {
//		int medoidX, medoidY;
//		BTree associations = new BTree();
//		int totalScore = 0;
//		private Cluster(int x, int y) {
//			medoidX = x;
//			medoidY = y;
//		}
//	}
	private static class Cluster {
		double lat, lng;
		BTree associations = new BTree();
		double totalScore = 0;
		private Cluster(double x, double y) {
			lat = x;
			lng = y;
		}
	}
	//			 med's      1  2
//	//           pos  0  1  2  3  4  5  6  7  8  9
//	static int[] x = {6, 6, 8, 1, 0, 9, 1, 4, 2, 4};
//	static int[] y = {1, 5, 7, 8, 1, 8, 6, 2, 4, 5};
	static double[] lats = {35.4627242, 33.5694041, 45.479984, 36.2197281, 33.4280652, 33.3503993, 36.0639767, 33.3938847, 40.1104457, 43.62453949, 50.9459599, 40.4066674, 43.10531009, 33.3038687, 36.263362, 35.9520457, 36.12573222, 33.4115139, 34.9811205, 33.3207235};
	static double[] lngs = {-80.8526119, -111.8902637, 73.58007, -115.1277255, -111.7266485, -111.8271417, -115.241463, -111.6822257, -88.2330726, -79.52910793, -114.0372072, -80.0044502, -89.5101418, -111.9516598, -115.1798386, -115.0934834, -115.1676084, -111.8953784, -80.9790228, -111.6858686};
	static final int NR_OF_CLUSTERS = 5;
	static final int NR_OF_RETRIES = 5;
	
	public static void main(String[] args) throws IOException {
//		rand = ((int) (Math.random() * lats.length));
//		Cluster clus1 = new Cluster(x[rand], y[rand]);
//		rand = ((int) (Math.random() * lats.length));
//		Cluster clus2 = new Cluster(x[rand], y[rand]);
//		double grandTotal = 0, testGrandTotal = 0;	// grand total used for determining if a randomly selected point acts as a better medoid than the previous
//		Cluster[] clusters = new Cluster[NR_OF_CLUSTERS];
//		
//		// get n random places to serve as initial medoids
//		for(int n = 0; n < clusters.length; n++) {
//			int rand = ((int) (Math.random() * lats.length));	// lats.length replace with bizIDS.length
//			clusters[n] = new Cluster(lats[rand], lngs[rand]);	// lats[] and lngs[] replaced by bizIDs[rand].latitude and bizIDs[rand].longitude
//		}
//		sift(clusters);
//		
//		for(int c = 0; c < clusters.length; c++) 
//			grandTotal += clusters[c].totalScore + clusters[c].totalScore;
//		
//		for(int i = 0; i < clusters.length; i++) {
//			for(int j = 0; j < NR_OF_RETRIES; j++) {
//				Cluster temp = randomNewMedoid(clusters);
//				Cluster[] tempClusters = new Cluster[clusters.length];
//				for(int k = 0; k < clusters.length; k++) 
//					tempClusters[k] = new Cluster(clusters[k].lat, clusters[k].lng);
//				tempClusters[i] = temp;
//				sift(tempClusters);
//				for(int m = 0; m < clusters.length; m++)
//					testGrandTotal += tempClusters[m].totalScore;
//				if(testGrandTotal < grandTotal) {
//					clusters = tempClusters;
//					grandTotal = testGrandTotal;
//				}
//			}
//			
//		}
//		// return clusters;
//		System.out.println();
		BusinessRecommender tets = new BusinessRecommender();
		String[] cluster;
//		cluster = tets.getCluster(tets.cluster[0].id);
		System.out.println();
	}

	private static Cluster randomNewMedoid(Cluster[] clusters) {
		boolean finding = true;
		
		while(finding) {
			finding = false;
			int rand = ((int) (Math.random() * lats.length));
			for(int i = 0; i < clusters.length; i++) {
				if(lats[rand] == clusters[i].lat && lngs[rand] == clusters[i].lng) {
					finding = true;
					break;
				}
			}
			if(finding == false) 
				return new Cluster(lats[rand], lngs[rand]);
		}
		return null;	// will never reach
	}
	
	// attach all nonmedoid points to the medoid closest to it.
	private static void sift(Cluster[] clusters) {
//		double compare1, compare2;
		
		// sift through all datapoints
		for(int i = 0; i < lats.length; i++) {
			// for all non-mediods..
//			compare1 = Math.abs(clusters[0].medoidX - x[i]) + Math.abs(clusters[0].medoidY - y[i]);
//			compare2 = Math.abs(clusters[1].medoidX - x[i]) + Math.abs(clusters[1].medoidY - y[i]);
			boolean nonmedoid = true;
			for(int j = 0; j < clusters.length; j++) {
				if(distance(clusters[j].lat, clusters[j].lng, lats[i], lngs[i]) == 0)	// if the datapoint we're currently at is a medoid
					nonmedoid = false;													// don't go through the logic to attach it
			}
			if(nonmedoid) {
				int index = compareDistances(clusters, lats[i], lngs[i]);	// find index of the nearest medoid in the clusters array
				clusters[index].associations.put(String.valueOf(i));	// String.valueOf(i) REPLACE WITH bizIds[index].key | add datapoint to that medoids associations
				clusters[index].totalScore += distance(clusters[index].lat, clusters[index].lng, lats[i], lngs[i]);	// add the distance to the total distance score of all attachments to that medoid
//				if(compare1 < compare2) {
//					clusters[0].associations.put(String.valueOf(i));
//					clusters[0].totalScore += compare1;
//				} else {
//					clusters[1].associations.put(String.valueOf(i));
//					clusters[1].totalScore += compare2;
//				}
			}
		}
	}
	
	private static int compareDistances(Cluster[] clusters, double dpLat, double dpLong) {
		Cluster currentSmallest = clusters[0];
		int currentSmallestIndex = 0;
		for(int i = 0; i < clusters.length; i++) {
			if(distance(clusters[i].lat, clusters[i].lng, dpLat, dpLong) < distance(currentSmallest.lat, currentSmallest.lng, dpLat, dpLong)) {
				currentSmallest = clusters[i];
				currentSmallestIndex = i;
			}
		}
		return currentSmallestIndex;
	}

	// returns distance between two points (where for each point, lat = x and long = y)
		private static double distance(double lat1, double long1, double lat2, double long2) {
			double d;
			double r = 6367 ;	// "nominal" radius of earth. approx. between the polar radius and equatorial radius
			d = 2 * r * (Math.asin(Math.sqrt(haversine(lat1, long1, lat2, long2))));
			return d;
		}
		
		private static double haversine(double lat1, double long1, double lat2, double long2) {
			double result;
			result = hav(lat2 - lat1) + Math.cos(lat1) * Math.cos(lat2) * hav(long2 - long1);
			return result;
		}

		private static double hav(double d) {
			double h;
			h = (1 - Math.cos(d)) / 2;
			return h;
		}

}
