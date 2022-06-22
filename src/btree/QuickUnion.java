package btree;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import btree.BTree.Node;

public class QuickUnion implements Serializable {
	private int[] id;
	private int[] sz;	// number of elements
	private int disjointSets;
	
	public QuickUnion(int N) {
		id = new int[N];
		sz = new int[N];
		disjointSets = N;
		for (int i = 0; i < N; i++) {
			id[i] = i;
			sz[i] = 1;
		}
	}
	
	private int root(int i) {
		while (i != id[i]) {
			id[i] = id[id[i]];	// Path Compression - attach to grandparent
			i = id[i];
		}
		return i;
	}
	
	public boolean find(int p, int q) {
		return root(p) == root(q);
	}
	
	public void unite(int p, int q) {
		int i = root(p);
		int j = root(q);
		if(i != j) { 
			disjointSets--; 	// only decrement total number of disjoint sets when both values don't have the same root
			if(sz[i] < sz[j]) {		// sz array + conditionals = Weighted Union
				id[i] = j;
				sz[j] += sz[i];
			} else {
				id[j] = i;
				sz[i] += sz[j];
			}
		}
	}
	
	public int disjointSets() { return disjointSets; }
	
	// Serialization 
	public static void diskWrite(QuickUnion qu) {
		try {
		    FileOutputStream file = new FileOutputStream("quickunion.ser");
		    ObjectOutputStream out = new ObjectOutputStream(file);
		    
		    // serialize object
		    //out.defaultWriteObject();
		    out.writeObject(qu);
		    
		    out.close();
		    file.close();
		}
		catch(IOException ex) {
		    System.out.println("IOException is caught");
		}
	}

}
