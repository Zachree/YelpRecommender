package btree.tests;

import btree.BusinessRecommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


public class QuickUnionTest {

	public static void main(String[] args) throws IOException {
		class Node {
			  Collection<Node> links;
			  Node disjointSet;
			  int disjointSetSize;
			  // ...
			  void initializeDisjointSet() {
				  disjointSet = this;	// the blank 'this' means equate disjointSet to this current whole Node object (point to itself)
				  disjointSetSize = 1;
			  }
			  
			  Node findDisjointSet() {	// find root
			    Node d = disjointSet, t = d;
			    while (t != (t = t.disjointSet)); // OK in Java, not C

			    while (d != t) { // compress
			      Node p = d.disjointSet; 
			      d.disjointSet = t; 
			      d = p;
			    }
			    return t;
			  }
			  
			  void unionDisjointSets(Node x, Node y) {
			    Node a = x.findDisjointSet(), b = y.findDisjointSet();
			    if (a != b) {
			      if (a.disjointSetSize < b.disjointSetSize) {
			         a.disjointSet = b;
			         b.disjointSetSize += a.disjointSetSize;
			  	 } else {
			         b.disjointSet = a;
			         a.disjointSetSize += b.disjointSetSize;
			      }
			    }
			  }
			}
		
		/*QuickUnion thing = new QuickUnion(10);
		thing.unite(3, 4);
		thing.unite(4, 9);
		thing.unite(8, 0);
		thing.unite(2, 3);
		thing.unite(5, 6);
		thing.unite(5, 9);
		thing.unite(4, 9);
		boolean pathExists = thing.find(2, 5);
		thing.unite(7, 3);
		thing.unite(4, 8);
		thing.unite(6, 1);
		
		Node node = new Node();*/
		
		BusinessRecommender test = new BusinessRecommender();
		System.out.println("Disjoint Sets: " + test.getDisjointSetsAmount());
		//double weight = bussin.getBusinessSimilarity("Target", "Jack in the Box");
		ArrayList<String> path = test.getPathToNearestCluster("v4qyBRpTBvOO55M4IAqNNg");
		for(int i = 0; i < path.size(); i++) {
			System.out.println(path.get(i));
		}
		int i = 1;
		
	}

}
