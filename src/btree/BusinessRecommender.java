package btree;
/* Hash Table (old, finished check-list)
 TODO
    -Make Hash Table general class
    -Clean reviews of non-word relevant characters (" *( ) & $ # @ ! ? / . , etc)
    -Make Hash Table of business names + id's
    -Make Hash Table of business id's + review text
    -Make Hash Table of words + term frequency across corpus & array of reviews containing the word used for the key
    -Make Array of all business names
    -GUI showing 5 or 10 or whatever random business names, and text field for user to enter name of the business they want to look for similar businesses of, 
        and a button to reshuffle what business names are displayed. After a business is entered, it will display 10 similar businesses to it and the name of the original business.
    -Use TF-IDF to find significant words for chosen business, then use the top scoring word to find other businesses that also score that word highly (Using the hash table with words for keys).
        If the top word dosen't have 10 similar enough businesses, do the same with the second highest scoring word, and if still under 10 similar businesses, again for the third
        highest scoring word, and so on, until 10 similar businesses are found.
    -Make Array to hold the ten names of the similar businesses to the chosen business.
 */
/* K-Medoid Clustering
	- get 5 random businesses as starting medoids
	- sift through all other businesses and compare geographic location to each of the starting medoids, pairing each non-medoid business
	with the medoid business that it's geographically closest to. When a business becomes associated to a medoid, add to a total that's unique
	to each medoid to track the cumulative distance between that medoid and all associated businesses.
	- somehow determine when to change the medoid to a different data point from among the already-associated data points, re-sift (through all 
	businesses to see if associations change), and see if its cumulative distance total is less than the previous medoid's total. If so, 
	replace that medoid with this "temp" one that was being tested. Do this for each medoid either a fixed number of times or until
	improvements aren't significant enough (like say, if the improvement is less than 10% better), or even both a fixed number of times and then
	continuing only if subsequent swaps are significant enough.
	- add persistancy to BTree and HashTables
	- update GUI to have buttons with the business names of the medoids, and if you click on one it'll pop up saying what the businesses in it's
	cluster are.
 */

import java.io.IOException;
import java.util.*;
import java.io.FileReader;

/**
 *
 * @author zachf
 */
public class BusinessRecommender {

	static class Medoid {	// holds the centerpoint for a cluster and also the cluster associated with that centerpoint(medoid)
		String id;
		double lat, lng;
		BTree cluster = new BTree();
		double totalScore = 0;
		private Medoid(String id, double x, double y) {
			this.id = id;
			lat = x;
			lng = y;
		}
	}
	
	private static class IDCoord {	// node for capturing a business and its geographic data for Haversine and clustering
		String id = "";
		double latitude, longitude;
		private IDCoord(String id, double lat, double lng) {
			this.id = id;
			latitude = lat;
			longitude = lng;
		}
	}
	
	private static class WordScore {
		String word;
		double score;
		private WordScore(String w, double s) { word = w; score = s; }
	}
	
	private static class Neighbor {
		IDCoord business = null;
		int index;			// index of this business in the IDCoords array. Used when finding disjoint sets via Quick Union.
		double distance;	// calculated distance from this business to the source that this object is neighboring.
		private Neighbor(IDCoord biz, int ind, double dist) { business = biz; index = ind; distance = dist; }
	}
	
	private static class NeighborSet {
		IDCoord key;											// business the neighbors are in relation to
		Neighbor[] neighbors = new Neighbor[NR_OF_NEIGHBORS];	// set of neighbors to this business
		int farthest = -1;										// index of the farthest neighbor from this business in neighbors, used for when finding neighbors so that the farthest neighbor is always removed when adding new closer neighbor
		int index;												// index of this business in array of all businesses
		private NeighborSet(IDCoord key, int index) { this.key = key; this.index = index; }
	}
	
	private static class business {
	    String name, id, latitude, longitude = "";
	}

	private static class review {
	    String body, id = "";
	}

    static final int NR_OF_DISPLAYED_BUSINESSES = 10;
    static final int NR_OF_TOP_WORDS = 10;  // fixme - from DL (kind of fixed - doesn't cause crashes anymore, but method still not a great approach)
    static final int NR_OF_SIMILAR_BUSINESSES = 5;
    static final int NR_OF_MEDOIDS = 5;
	static final int NR_OF_RETRIES = 5;		// how many times to try a new medoid for each initial medoid
	static final int NR_OF_NEIGHBORS = 4;	// amount of nearest geographical neighbors to find
    
    public HashTable IDTable, reviewTable, neighborHT;
    public WordFreqHT allReviewWords;
    //public BTree IDTree;
    public static Medoid[] medoids;
    public static String nearestBusiness;
    static int loadedClusters = 1;
    public static NeighborSet[] sets;

    public BusinessRecommender() throws IOException {
        this.reviewTable = createReviewTable();
        this.IDTable = createIDTable();
        this.allReviewWords = createAllWordsTable(reviewTable);
        //this.IDTree = createIDBTree(IDTable);		// make BTree of all businesses. not needed but was made for proof of concept
        BusinessRecommender.medoids = createClusters();
    }

	public String[] getRecommendations(String business) throws IOException {
        String[] topWords, similarBusinesses;
        HashTable termsTFIDF;
        String businessID;
        
        businessID = getBusinessID(business, IDTable, reviewTable);
        termsTFIDF = createTermsTFIDF(businessID, allReviewWords, reviewTable);
        topWords = calculateTopWords(termsTFIDF);
        similarBusinesses = getSimilarBusinesses(topWords, business, allReviewWords, IDTable);
        
        return similarBusinesses;
    }
	
	public ArrayList<String> getPathToNearestCluster(String startingPointID) {
		//createNeighborHT();
		Collection<Node> vertecies = new ArrayList<>();
		Collection<Edge> edges;
		Node vertex, dst, nearestMedoid = null, root = null, d;
		Edge edge;
		ArrayList<String> path;
		double weight, currentNearestDistance = Double.MAX_VALUE, contenderDistance, w;
		Graph graph;
		String bizName, neighborName;
		// loop through sets array and create vertex Nodes containing edge sets so the Node has edges for each connected neighbor in accordance with the sets array, adding the 
		// connection weights while creating the vertex Node.
		for(NeighborSet set : sets) {
			edges = new ArrayList<>();
			bizName = IDTable.get(set.key.id).toString();
			vertex = new Node(bizName);
			if(startingPointID.equals(set.key.id)) { root = vertex; }
			for (Medoid md : medoids) {	// check each vertex for if it's a cluster medoid. if so, update if it's the nearest 
				if (vertex.name == IDTable.get(md.id)) {
					if (nearestMedoid == null) {
						nearestMedoid = vertex;
						currentNearestDistance = distance(IDTable.getLatitude(startingPointID), IDTable.getLongitude(startingPointID), IDTable.getLatitude(md.id), IDTable.getLongitude(md.id));
					} else {
						contenderDistance = distance(IDTable.getLatitude(startingPointID), IDTable.getLongitude(startingPointID), IDTable.getLatitude(md.id), IDTable.getLongitude(md.id));
						if(contenderDistance < currentNearestDistance) {
							nearestMedoid = vertex;
							currentNearestDistance = contenderDistance;
						}
					}
				}
			}
			for(Neighbor nbr : set.neighbors) {
				neighborName = IDTable.get(nbr.business.id).toString();
				dst = new Node(neighborName);
				/*nn = (NeighborSet)neighborHT.get(nbr.business.id);
				for(Neighbor n : nn.neighbors) {
					d = new Node(IDTable.get(nbr.business.id).toString());
					w = similarity(IDTable.get(nbr.business.id).toString(), IDTable.get(n.business.id).toString());
					edge = new Edge(dst, d, w);
					edges.add(edge);
				}
				dst.edges = edges;*/
				edges = new ArrayList<>();
				weight = similarity(bizName, neighborName);
				edge = new Edge(vertex, dst, weight);
				edges.add(edge);
				if(vertex.best == 0) { vertex.best = weight; }
				if(weight < vertex.best) { vertex.best = weight; }
			}
			vertex.edges = edges;
			vertecies.add(vertex);
		}
		graph = new Graph(vertecies, nearestMedoid);
		path = (ArrayList<String>)graph.buildShortestPathTree(root);
		//Collections.reverse(path);
		return path;
	}

	// Calculates the similarity (a number within 0 and 1) between two strings.
	public static double similarity(String s1, String s2) {
		String longer = s1, shorter = s2;
		if (s1.length() < s2.length()) { // longer should always have greater length
			longer = s2;
			shorter = s1;
		}
		int longerLength = longer.length();
		if (longerLength == 0) { // both strings are zero length
			return 1.0;
		}
		return (1 - ((longerLength - editDistance(longer, shorter)) / (double) longerLength));

	}

	// Levenshtein Edit Distance
	public static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

	public int getDisjointSetsAmount() {
		return quickUnionNeighbors(IDTable, reviewTable);
	}
	
	private static int quickUnionNeighbors(HashTable iT, HashTable rT) {
		IDCoord[] bizCoords = createBusinessIDCoordArray(rT, iT);
		sets = new NeighborSet[bizCoords.length];
		QuickUnion connections = new QuickUnion(bizCoords.length);
		double dist;
		int neighborIndex = 0;
		
		for (int i = 0; i < bizCoords.length; i++) {
			sets[i] = new NeighborSet(bizCoords[i], i);
			for(int j = 0; j < bizCoords.length; j++) {
				if(!(sets[i].key.id.equals(bizCoords[j].id))) {
					if(sets[i].neighbors[3] == null) {	// initial setting of neighbors
						dist = distance(sets[i].key.latitude, sets[i].key.longitude, bizCoords[j].latitude, bizCoords[j].longitude);
						sets[i].neighbors[neighborIndex] = new Neighbor(bizCoords[j], j, dist);
						neighborIndex++;
						if(sets[i].farthest == -1)
							sets[i].farthest = j;
						findFarthest(sets[i]);
					}
					else
						updateFarthest(sets[i], bizCoords[j], j);
				}
			}
			neighborIndex = 0;
			for(int k = 0; k < sets[i].neighbors.length; k++) {		// using indexes, unite current business to its neighbors
				if(sets[i].neighbors[k] != null)
					connections.unite(i, sets[i].neighbors[k].index);
			}
		}
		QuickUnion.diskWrite(connections);
		return connections.disjointSets();
	}

	private static void updateFarthest(NeighborSet set, IDCoord business, int bizCoordIndex) {
		double contenderDistance = distance(set.key.latitude, set.key.longitude, business.latitude, business.longitude);
		double currentFarthestDistance = distance(set.key.latitude, set.key.longitude, set.neighbors[set.farthest].business.latitude, set.neighbors[set.farthest].business.longitude);
		if(currentFarthestDistance > contenderDistance) {
			set.neighbors[set.farthest] = new Neighbor(business, bizCoordIndex, contenderDistance);
			findFarthest(set);
		}
	}

	//loop through neighbors to find the new current farthest, and update farthest
	private static void findFarthest(NeighborSet set) {
		int i = 0, farthest = 0;
		while(i < 3 && set.neighbors[i+1] != null) {
			if(set.neighbors[i+1].distance > set.neighbors[farthest].distance)
				farthest = i+1;
			i++;
		}
		set.farthest = farthest;
	}
	
	// probably a good similarity metric - too slow though to be worth doing for every edge for a graph.  
	// compares TFIDF score tables of two businesses, accumulating the scores for words in common between the two businesses and returning the accumulation (total score).
	public double getBusinessSimilarity(String business1, String business2) {
		String businessID1, businessID2;
		WordScore[] sortedTerms1, sortedTerms2;
		HashTable termsTFIDF1, termsTFIDF2;
		double weight;
		
		businessID1 = getBusinessID(business1, IDTable, reviewTable);
		businessID2 = getBusinessID(business2, IDTable, reviewTable);
        termsTFIDF1 = createTermsTFIDF(businessID1, allReviewWords, reviewTable);
        termsTFIDF2 = createTermsTFIDF(businessID2, allReviewWords, reviewTable);
        sortedTerms1 = getTFIDFArray(termsTFIDF1);
        sortedTerms2 = getTFIDFArray(termsTFIDF2);
        if(sortedTerms1.length < sortedTerms2.length) {
        	weight = compareReviewSimilarity(sortedTerms1, sortedTerms2);		// use smaller set of words for the outer loop in compareReviewSimilarity. not sure if it matters, probably not.
        } else {
        	weight = compareReviewSimilarity(sortedTerms2, sortedTerms1);
        }
        return weight;
	}
	
	private static double compareReviewSimilarity(WordScore[] s1, WordScore[] s2) {
		double w = 0;
		for(WordScore word1 : s1) {
			for(WordScore word2 : s2) {
				if(word1.word.equals(word2.word)) {
					w += word1.score + word2.score;
					break;
				}
			}
		}
		return w;
	}
	
	// create a sorted array of words for passed in review's words, sorted by their tfidf score. (same as 
    private static WordScore[] getTFIDFArray(HashTable termsTFIDF) {
        WordScore[] sortedTFIDFWords = new WordScore[termsTFIDF.count];
        int i = 0;
        // fill up an array with all the words of the given review
        for(HashTable.Node term : termsTFIDF.table){
            if(term != null){
                sortedTFIDFWords[i] = new WordScore(term.key.toString(), (double)term.val);
                i++;
            }
        }
        return sortedTFIDFWords;
    }

	// return the inorder array of keys of the BTree of the cluster associations for the passed-in business ID
	public String[] getCluster(String bizID) {
		return getClusterHelper(bizID, IDTable, reviewTable);
	}
	
	private static String[] getClusterHelper(String bizID, HashTable IDTable, HashTable reviewTable) {
		String[] associationsIDs, associationsNames;
		Object name = null;
		for(int i = 0; i < medoids.length; i++) {
			if(medoids[i].id.equals(bizID)) {
				medoids[i].cluster.getInorder(medoids[i].cluster.root, medoids[i].cluster.height);
				associationsIDs = BTree.sortedKeys;
				associationsNames = new String[associationsIDs.length];
				for(int j = 0; j < associationsIDs.length; j++) {
					if(associationsIDs[j] != null)
						name = IDTable.get(associationsIDs[j]);
					else
						associationsNames[0] = "Empty Cluster";
					if(name != null)
						associationsNames[j] = name.toString();
				}
				findNearestBusiness(bizID, IDTable);		// placed here because this method is what the button uses
				return associationsNames;
			}
		}
		return null;
	}
	
	// finds the nearest business to the requested medoid
	private static void findNearestBusiness(String medoidID, HashTable IDTable) {
		String tempNearest;
		String[] cluster = null;
		double currentNearest, contender;
		// find the medoid node and use it to generate the string array of all the keys in its cluster
		for(int i = 0; i < medoids.length; i++) {
			if(medoids[i].id.equals(medoidID)) {
				medoids[i].cluster.getInorder(medoids[i].cluster.root, medoids[i].cluster.height);
				cluster = BTree.sortedKeys;
				break;
			}
		}
		nearestBusiness = cluster[0];
		// compare the distances of all businesses in the cluster to the medoid to find the nearest
		for(int j = 1; j < cluster.length; j++) {
			tempNearest = cluster[j];
			currentNearest = distance(IDTable.getLatitude(nearestBusiness), IDTable.getLongitude(nearestBusiness), IDTable.getLatitude(medoidID), IDTable.getLongitude(medoidID));
			contender = distance(IDTable.getLatitude(tempNearest), IDTable.getLongitude(tempNearest), IDTable.getLatitude(medoidID), IDTable.getLongitude(medoidID));
			if(contender < currentNearest)
				nearestBusiness = tempNearest;
		}
		nearestBusiness = IDTable.get(nearestBusiness).toString();
	}
	
	// return an array of the business names of the medoids
	public String[] getMedoidNames() {
		return getMedoidNamesHelper(IDTable);
	}
	
	private static String[] getMedoidNamesHelper(HashTable IDTable) {
		String[] medoidsNames = new String[medoids.length];
		for(int i = 0; i < medoids.length; i++) {
			medoidsNames[i] = IDTable.get(medoids[i].id).toString();
		}
		return medoidsNames;
	}
	
	// return an array of the business ID's of the medoids
	public String[] getMedoidIDs() {
		String[] medoidIDs = new String[medoids.length];
		for(int i = 0; i < medoids.length; i++) {
			medoidIDs[i] = medoids[i].id;
		}
		return medoidIDs;
	}
	
	public Medoid[] createClusters() {
		return createClustersHelper(IDTable, reviewTable);
	}
	
	// return array of all clusters generated from the business ID's
	public static Medoid[] createClustersHelper(HashTable IDTable, HashTable reviewTable) {
		IDCoord[] bizCoords = createBusinessIDCoordArray(reviewTable, IDTable);
		double grandTotal = 0, testGrandTotal = 0;	// grand total used for determining if a randomly selected point acts as a better medoid than the previous
		medoids = new Medoid[NR_OF_MEDOIDS];
		
		// get n random places to serve as initial medoids
		for(int n = 0; n < medoids.length; n++) {
			int rand = ((int) (Math.random() * bizCoords.length));
			medoids[n] = new Medoid(bizCoords[rand].id, bizCoords[rand].latitude, bizCoords[rand].longitude);
		}
		
		sift(medoids, bizCoords);
		for(int c = 0; c < medoids.length; c++) 
			grandTotal += medoids[c].totalScore + medoids[c].totalScore;
		
		for(int i = 0; i < medoids.length; i++) {
			for(int j = 0; j < NR_OF_RETRIES; j++) {
				Medoid temp = randomNewMedoid(medoids, bizCoords);
				Medoid[] tempMedoids = new Medoid[medoids.length];
				for(int k = 0; k < medoids.length; k++) 
					tempMedoids[k] = new Medoid(medoids[k].id, medoids[k].lat, medoids[k].lng);
				tempMedoids[i] = temp;
				sift(tempMedoids, bizCoords);
				for(int m = 0; m < medoids.length; m++)
					testGrandTotal += tempMedoids[m].totalScore;
				if(testGrandTotal < grandTotal) {
					medoids = tempMedoids;
					grandTotal = testGrandTotal;
				}
				System.out.println("Solving Clusters... " + loadedClusters + "/25");
				loadedClusters++;
			}
		}
		return medoids;
	}
	
	// return a randomly-selected nonmedoid-datapoint (for comparison to an existing medoid to see if it makes the overall connections better)
	private static Medoid randomNewMedoid(Medoid[] medoids, IDCoord[] bizCoords) {
		boolean finding = true;
		
		while(finding) {
			finding = false;
			int rand = ((int) (Math.random() * bizCoords.length-1));
			for(int i = 0; i < medoids.length; i++) {
				if(bizCoords[rand].latitude == medoids[i].lat && bizCoords[rand].longitude == medoids[i].lng) {
					finding = true;
					break;
				}
			}
			if(!finding)
				return new Medoid(bizCoords[rand].id, bizCoords[rand].latitude, bizCoords[rand].longitude);
		}
		return null;	// fall through
	}
	
	// attach all nonmedoid points to the medoid closest to it
	private static void sift(Medoid[] medoids, IDCoord[] bizCoords) {
		// sift through all datapoints
		for(int i = 0; i < bizCoords.length; i++) {
			boolean nonmedoid = true;
			// for all non-mediods..
			for(int j = 0; j < medoids.length; j++) {
				if(distance(medoids[j].lat, medoids[j].lng, bizCoords[i].latitude, bizCoords[i].longitude) == 0) {	// if the datapoint we're currently at is a medoid.. (could be improved by setting up an if bracket/switch statement where the conditionals are each medoid, returning true if they match or false if it falls through)
					nonmedoid = false;													// don't go through the logic to attach it
					break;
				}
			}
			if(nonmedoid) {
				int index = findNearestMedoid(medoids, bizCoords[i]);	// find index of the nearest medoid in the clusters array
				//System.out.println(i);
				medoids[index].cluster.put(bizCoords[i].id);	// add datapoint to that medoids associations
				medoids[index].totalScore += distance(medoids[index].lat, medoids[index].lng, bizCoords[i].latitude, bizCoords[i].longitude);	// add the distance to the total distance score of all attachments to that medoid
			}
		}
	}
	
	// finds the medoid with the shortest distance to the passed in business' coordinates
	private static int findNearestMedoid(Medoid[] medoids, IDCoord bizCoord) {
		Medoid currentSmallest = medoids[0];
		int currentSmallestIndex = 0;
		double contenderMedoidDistance, currentNearestMedoidDistance;
		for(int i = 0; i < medoids.length; i++) {
			contenderMedoidDistance = distance(medoids[i].lat, medoids[i].lng, bizCoord.latitude, bizCoord.longitude);
			currentNearestMedoidDistance = distance(currentSmallest.lat, currentSmallest.lng, bizCoord.latitude, bizCoord.longitude);
			if(contenderMedoidDistance < currentNearestMedoidDistance) {
				currentSmallest = medoids[i];
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
    
	// create a BTree holding all businesses
    /*private BTree createIDBTree(HashTable IDTable) {
    	BTree IDs = new BTree();
        String bizID;
        int i = 0;
        for (HashTable.Node slot : IDTable.table) {
            if (slot != null) {
            	System.out.println(i);
            	i++;
                bizID = slot.key.toString();
                IDs.put(bizID);
            }
        }
		return IDs;
	}*/
    
    // create table of all business ID's and the business name associated with that ID.
    private static HashTable createIDTable() throws IOException {
        FileReader file = new FileReader("yelp_academic_dataset_business_w_locations.csv");
        Scanner scanner = new Scanner(file);
        Scanner dataScanner = new Scanner(scanner.nextLine());
        int index = 0;
        int nrOfBusinesses = 30000;    // with 30k businesses scanned, there's only 1 review (of 10k) whose business id wasn't scanned. i accept this.
        HashTable Businesses = new HashTable();
        // first couple lines of the file are junk for our purposes, so skip them
        dataScanner.close();
        dataScanner = new Scanner(scanner.nextLine());
        dataScanner.close();
        dataScanner = new Scanner(scanner.nextLine());
        
        while (index < nrOfBusinesses) {
            dataScanner.useDelimiter(">");
            business biz = new business();
            biz.name = dataScanner.next();
            biz.id = dataScanner.next();
            biz.latitude = dataScanner.next();
            biz.longitude = dataScanner.next();
            biz.name = biz.name.replaceAll("[,\"]*", "");   // clean up the name, get rid of commas and quotation marks
            biz.id = biz.id.replaceAll("[,']", "");          // clean up the id, get rid of commas
            biz.latitude = biz.latitude.replaceAll("[,']", "");          // clean up the id, get rid of commas
            biz.longitude = biz.longitude.replaceAll("[,']", "");          // clean up the id, get rid of commas
            Businesses.put(biz.id, biz.name, Double.parseDouble(biz.latitude), Double.parseDouble(biz.longitude));
            index++;
            if(scanner.hasNextLine()){  // if not at end of file, skip two lines down (because there's a blank line between each listing)
                dataScanner = new Scanner(scanner.nextLine());
                dataScanner.close();
                dataScanner = new Scanner(scanner.nextLine());
            }
        }
        scanner.close();
        dataScanner.close();
        return Businesses;
    }
    
    // compare the IDTable and reviewTable to each other and create a new table of only the business names + respective IDs for the businesses we actually have a review for.
    private static HashTable createBusinessTable(HashTable IDTable) {
        HashTable IDs = new HashTable();
        String bizName, bizID;
        for (HashTable.Node biz : IDTable.table) {
            if (biz != null) {
                bizName = biz.val.toString();
                bizID = biz.key.toString();
                IDs.put(bizName, bizID);
            }
        }
        HashTable.diskWrite(IDs.table);
        return IDs;
    }
    
    // create table of business ID's and the text of ALL reviews associated with that business ID.
    private static HashTable createReviewTable() throws IOException {
        // Scanner has issues just providing the path to the file (due to size),
        // so explicitly making the file a FileReader object fixes said issues
        FileReader file = new FileReader("yelp_academic_dataset_review.csv");
        Scanner scanner = new Scanner(file);
        Scanner dataScanner;
        int index = 0;
        int nrOfReviews = 10000;
        boolean endOfReview = false;
        boolean textFound = false;
        String token;
        HashTable Reviews = new HashTable();
        
        while (index < nrOfReviews) {   // get however many reviews
            dataScanner = new Scanner(scanner.nextLine());  // first line of the file is junk for our purposes, so skip that first line and then each other line after, due to empty lines
            dataScanner.useDelimiter(",");  
            review rev = new review();
            
            // doing this first gets rid of garbage at the start of the parsed review, since we're appending each token in the loop constructing the review body
            token = dataScanner.next();
            rev.body = token;
            
            while (!endOfReview) {      // dynamic loop to retrieve all review body contents
                while (!textFound) {    // dynamic find next non-empty line of text while-loop (needed bc of varying number/placement of new-lines, depending on the commenter)
                    dataScanner.useDelimiter(",");
                    if (!dataScanner.hasNext()) {
                        dataScanner = new Scanner(scanner.nextLine());
                    } else
                        textFound = true;
                }
                textFound = false;  // reset for next empty line encounter
                dataScanner.useDelimiter(",");
                token = " " + dataScanner.next();
                if(!token.contains(">")) {  // '>' was used as a unique character to indicate the split between the review and ID
                    rev.body += token;
                } else {
                    endOfReview = true;
                }
            }
            endOfReview = false;    // reset for next review
            rev.id = dataScanner.next();
            rev.body = rev.body.replaceAll("[^a-zA-Z0-9]", " ").toLowerCase();   // clean up review body of all special char's and make all lower case, for better frequency counting
            Reviews.put(rev.id, rev.body);
            index++;
        }
        scanner.close();
        HashTable.diskWrite(Reviews.table);
        return Reviews;
    }
    
    // make array of all business names that we have reviews for.
    private static String[] createBusinessNamesArray(HashTable reviewTable, HashTable IDTable){
        String[] businessNames = new String[4397];    // 4397 total unique businesses among the 10k reviews.
        int p = 0;
        String bizID;
        Object bizName;
        for(int i = 0; i < reviewTable.table.length-1; i++){
            if (reviewTable.table[i] != null) {
                bizID = reviewTable.table[i].key.toString();
                bizName = IDTable.get(bizID);
                if(bizName != null)
                    businessNames[p] = bizName.toString();
                else
                    businessNames[p] = "The business this review is for does not exist in the IDTable.";
                //System.out.println(i + " " + businessNames[p]);
                p++;
            }
        }
        return businessNames;
    }
    
    // make array of all business IDs that we have reviews for, with geographic info attached.
    private static IDCoord[] createBusinessIDCoordArray(HashTable reviewTable, HashTable IDTable){
        IDCoord[] businessIDs = new IDCoord[4338];    // 4338 total unique businesses among the 10k reviews
        IDCoord biz;
        Object bizName;
        String id;
        int p = 0;
        for(int i = 0; i < reviewTable.table.length-1; i++){
            if (reviewTable.table[i] != null) {
            	id = reviewTable.table[i].key.toString();
            	biz = new IDCoord(id, IDTable.getLatitude(id), IDTable.getLongitude(id));
                bizName = IDTable.get(biz.id);
                if(bizName != null) {
                	businessIDs[p] = biz;
                    p++;
                }
            }
        }
        return businessIDs;
    }
    private void createNeighborHT() {
    	neighborHT = new HashTable();
    	for(int i = 0; i < sets.length; i++) {
    		neighborHT.put(sets[i].key.id, sets[i]);
    	}
    }
    public static String[] getRandomBusinesses() throws IOException{
        HashTable IDTable = createIDTable();
        HashTable  reviewTable = createReviewTable();
        String [] businessNames = createBusinessNamesArray(reviewTable, IDTable);
        String[] randomBusinesses = new String[10];
        int rand;
        for(int i = 0; i < NR_OF_DISPLAYED_BUSINESSES; i++){
            rand = (int) (Math.random() * businessNames.length);
            randomBusinesses[i] = businessNames[rand];
            //System.out.println(randomBusinesses[i]);
        }
        return randomBusinesses;
    }

    // create table where each key = a word (from all reviews, no duplicates) and value = a single string containing each business ID where the key was found in.
    private static WordFreqHT createAllWordsTable(HashTable reviewTable) {
        WordFreqHT allWords = new WordFreqHT();
        String review;
        String word;
        String bizID;
        Scanner words = null;
        for (HashTable.Node slot : reviewTable.table) {
            if (slot != null) {
                review = slot.val.toString();
                words = new Scanner(review);
                while(words.hasNext()) {
                    word = words.next();
                    bizID = slot.key.toString();
                    allWords.put(word, bizID);
                }
            }
        }
        words.close();
        WordFreqHT.diskWrite(allWords.table);
        return allWords;
    }

    // create table of business ID's with a word frequency table of the words of the reviews for that business.
    private static TermFreqHT createTermFreqTable(HashTable reviewTable) {
        TermFreqHT termFreqTable = new TermFreqHT();
        String review;
        String word;
        String bizID;
        Scanner words;
        for (HashTable.Node slot : reviewTable.table) {
            if (slot != null) {
                review = slot.val.toString();
                words = new Scanner(review);
                while(words.hasNext()) {
                    word = words.next();
                    bizID = slot.key.toString();
                    termFreqTable.put(bizID, word);
                }
            }
        }
        TermFreqHT.diskWrite(termFreqTable.table);
        return termFreqTable;
    }

    // create table each word and the tfidf score for that word.
    private static HashTable createTermsTFIDF(String businessID, WordFreqHT allReviewWords, HashTable reviewTable) {
        double sum = 0;
        double nrOfDocuments = reviewTable.count;
        double nrOfDocsContainingWord, tf, idf, tfidf;
        TermFreqHT termFreqTable = createTermFreqTable(reviewTable);
        WordFreqHT wordFreq = termFreqTable.get(businessID);
        HashTable termsTFIDF = new HashTable();
        
        for (WordFreqHT.Node word : wordFreq.table) {
            if (word != null) {
                sum += word.num;
            }
        }
        for (WordFreqHT.Node word : wordFreq.table) {
            if (word != null) {
                nrOfDocsContainingWord = calculateNrOfDocsContainingWord(word.key, allReviewWords);
                tf = word.num / sum;
                idf = Math.log(nrOfDocuments) / nrOfDocsContainingWord;
                tfidf = tf * idf;
                termsTFIDF.put(word.key, tfidf);
            }
        }
        HashTable.diskWrite(termsTFIDF.table);
        return termsTFIDF;
    }
    
    // create a sorted array of words for passed in review's words, sorted by their tfidf score.
    private static String[] calculateTopWords(HashTable termsTFIDF) {
        String[] sortedTFIDFWords = new String[termsTFIDF.count];
        String[] topWords = new String[NR_OF_TOP_WORDS];
        int i = 0;
        String temp;
        // fill up an array with all the words of the given review
        for(HashTable.Node termTFIDF : termsTFIDF.table){
            if(termTFIDF != null){
                sortedTFIDFWords[i] = termTFIDF.key.toString();
                i++;
            }
        }
        // bubble sort the array of all the words according to their tfidf scores
        for(i = 0; i < sortedTFIDFWords.length; i++){
            for(int j = 0; j < sortedTFIDFWords.length; j++){
                double tfidf1 = Double.parseDouble(termsTFIDF.get(sortedTFIDFWords[i]).toString());
                double tfidf2 = Double.parseDouble(termsTFIDF.get(sortedTFIDFWords[j]).toString());
                if(tfidf1 > tfidf2){
                    temp = sortedTFIDFWords[i];
                    sortedTFIDFWords[i] = sortedTFIDFWords[j];
                    sortedTFIDFWords[j] = temp;
                }
            }
        }
        /*for (String sortedTFIDFWord : sortedTFIDFWords) {
            System.out.println(termsTFIDF.get(sortedTFIDFWord).toString());
        }*/
        for(i = 0; i < NR_OF_TOP_WORDS; i++){
            topWords[i] = sortedTFIDFWords[i];
        }
        return topWords;
    }

    // get the amount of business ID's that have the desired word in their reviews.
    private static double calculateNrOfDocsContainingWord(Object word, WordFreqHT allReviewWords) {
        double nrOfBusinesses = 0;
        String businessIDs = allReviewWords.get(word).toString();
        Scanner scanner = new Scanner(businessIDs);
        while(scanner.hasNext()){
            ++nrOfBusinesses;
            scanner.next();
        }
        scanner.close();
        
        return nrOfBusinesses;
    }
    
    // get the business ID for the desired business name.
    private static String getBusinessID(String business, HashTable IDTable, HashTable reviewTable) {
        HashTable businessTable = createBusinessTable(IDTable);
        String allIDsOfBusiness, oneBusinessID;
        allIDsOfBusiness = businessTable.get(business).toString();
        Scanner scan = new Scanner(allIDsOfBusiness);
        while(true){
            oneBusinessID = scan.next();
            if(reviewTable.get(oneBusinessID) != null) {  // only return a business ID that we actually have a review for.
            	scan.close();
                return oneBusinessID;
            }
        }
    }

    // get array of n similar businesses to the desired business name.
    private static String[] getSimilarBusinesses(String[] topWords, String businessName, WordFreqHT allReviewWords, HashTable IDTable) {
        String[] similarBusinesses = new String[NR_OF_SIMILAR_BUSINESSES];
        String businesses, word, oneBusinessID;
        Scanner scan;
        boolean wordExhausted = false;
        int i = 0;
        int similarBusinessesFound = 0;
        while(similarBusinessesFound < NR_OF_SIMILAR_BUSINESSES && i < NR_OF_TOP_WORDS){
            word = topWords[i];
            businesses = allReviewWords.get(word).toString();
            scan = new Scanner(businesses);
            while(!wordExhausted){
                if(scan.hasNext()){
                    oneBusinessID = scan.next();
                    if(IDTable.get(oneBusinessID) != null && similarBusinessesFound < NR_OF_SIMILAR_BUSINESSES && !businessName.equals(IDTable.get(oneBusinessID).toString())){
                        similarBusinesses[similarBusinessesFound] = IDTable.get(oneBusinessID).toString();
                        ++similarBusinessesFound;
                    }
                }
                else{
                    wordExhausted = true;
                    ++i;
                }
            }
            wordExhausted = false;
        }
        return similarBusinesses;
    }
}

// everything below was taken from Doug's csc365-weighted document - with slight edits
class Node implements Comparable<Node> { // graph vertex
	  // ...
	  Collection<Edge> edges;
	  double best = 0;
	  int pqIndex;
	  Node parent;
	  String name;
	  Node(String n) { name = n; }
	  public int compareTo(Node x) { return Double.compare(best, x.best); }
}

class Edge {
	Node src, dst;
	double weight;
	Edge(Node s, Node d, double w) { src = s; dst = d; weight = w; }
}

// Prim minimum spanning tree graph implementation
/*class Graph {
	Collection<Node> nodes;
	// �
	void buildMinimumSpanningTree(Node root) {
		PQ pq = new PQ(nodes, root);
	    Node p;
	    while ((p = pq.poll()) != null) { 
	      for (Edge e : p.edges) {
	        Node s = e.src, d = e.dst; 
	        double w = e.weight;
	        if (w < d.best) {
	          d.parent = s;
	          d.best = w;
	          pq.resift(d);
	        }
	      }
	    }
	}
}*/

// Dijkstra graph implementation
class Graph {
	Collection<Node> nodes;
	Node destination;	// SET/CHANGE ME when creating Graph object
	Graph(Collection<Node> nodes, Node destination) { this.nodes = nodes; this.destination = destination; }

	Collection<String> buildShortestPathTree (Node root) {
		PQ pq = new PQ(nodes, root);
		Node p;
		Node nextNode = null;
		ArrayList<String> path = new ArrayList<>();
		path.add(root.name);
		while ((p = pq.poll()) != null) {
			if (p == destination)	// single-source destination implementation. if we're only looking for a single path, this will pathfind until finding the destination.
				break; 
			for (Edge e : p.edges) {
				if(nextNode == null) { nextNode = e.dst; }	// initialize nextNode in case the first edge ends up being the best
				Node s = e.src, d = e.dst;
				d.best = e.weight;
				double w = s.best + e.weight; // was: w = e.weight;
				if (w < d.best) {
					d.parent = s;
					d.best = w;
					pq.resift(d);
					nextNode = d;	// update nextNode whenever a better path is found
				}
			}
			path.add(nextNode.name); // add the destination of the best edge to the path
		}
		return path;
	}
}

class PQ {
	final Node[] array;
	int size;

	PQ(Collection<Node> nodes, Node root) {
		array = new Node[nodes.size()];
		root.best = 0;
		root.pqIndex = 0;
		array[0] = root;
		int k = 1;
		for (Node p : nodes) {
			p.parent = null;
			if (p != root) {
				p.best = Double.MAX_VALUE;
				array[k] = p;
				p.pqIndex = k++;
			}
		}
		size = k;
	}

	void resift(Node x) {
		int k = x.pqIndex;
		assert (array[k] == x);
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			Node p = array[parent];
			if (x.compareTo(p) >= 0)
				break;
			array[k] = p;
			p.pqIndex = k;
			k = parent;
		}
		array[k] = x;
		x.pqIndex = k;
	}

	void add(Node x) { // unused; for illustration
		x.pqIndex = size++;
		resift(x);
	}

	Node poll() {
		int n = size;
		Edge[] eg;
		if (n == 0)
			return null;
		Node least = array[0];
		if (least.best == Double.MAX_VALUE) {
			return null;
			//eg = (Edge[])least.edges.toArray();	// set least.best to weight??
			//least.best = eg[0].weight;
		}
		size = --n;
		if (n > 0) {
			Node x = array[n];
			array[n] = null;
			int k = 0, child; 
			// while at least a left child
			while ((child = (k << 1) + 1) < n) {
				Node c = array[child];
				int right = child + 1;
				if (right < n) {
					Node r = array[right];
					if (c.compareTo(r) > 0) {
						c = r;
						child = right;
					}
				}
				// if (x.compareTo((T) c) <= 0)
				if (x.compareTo(c) <= 0)
					break;
				array[k] = c;
				c.pqIndex = k;
				k = child;
			}
			array[k] = x;
			x.pqIndex = k;
		}
		return least;
	}
}
