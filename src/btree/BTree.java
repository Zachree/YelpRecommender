package btree;

import java.io.*;

public class BTree {
	
	// Max number of children. Max number of elements in a node = MAX-1
	static private final int MAX = 101;

	public int totalKeys = 0;
	public int height;
	static String[] sortedKeys;
	static int index;
	public Node root;
	
	static class Node /* implements Serializable */ {
        int nrOfElements = 0;
        Seed[] keys = new Seed[MAX];
        
        private Node(int n){
            nrOfElements = n;
        }
	}
	
	private static class Seed /* implements Serializable */ {
		String key;
		Node leftChild = null;
		Node rightChild = null;
		
		private Seed(String k) {
			key = k;
		}
		private Seed(String k, Node l, Node r) {
			key = k;
			leftChild = l;
			rightChild = r;
		}
	}
	
	public BTree() {
		root = new Node(0);
		height = 0;
		//diskWrite(root);
	}
	
	public void put(String key) {
		//Node root = diskRead();
		Node u = insert(root, key, height);
		totalKeys++;
		if (u == null) {
			//diskWrite(root);
			return;
		}
		
		// need to split root
        Node t = new Node(1);
        // keys were being duplicated. when root is split, the left subtree is just made from the full root 
        // without removing the keys that were supposed to be split into the right subtree. The right subtree is
        // all new, made only from the latter half of the root, so it's all good. this for loop just removes the
        // keys that were moved into the right subtree
        String middle = root.keys[MAX/2].key;
        for(int i = root.keys.length-1; i > MAX/2-1; i--) { root.keys[i] = null; }
        t.keys[0] = new Seed(middle, root, u);
        // DISK-WRITE(t)
        // DISK-WRITE(t.key[0].leftChild)
        // DISK-WRITE(t.key[0].rightChild)
        // close disk writer
        root = t;
        height++;
        //diskWrite(root);
	}
	
	private Node insert(Node nd, String key, int ht) {
		int j;
		Node newChild = null;
		Seed sd = new Seed(key);
		
		// leaf node
		if(ht == 0) {
			for(j = 0; j < nd.nrOfElements; j++) {
				if(less(key, nd.keys[j].key))
					break;
			}
		}
		// internal node
		else {
			for(j = 0; j < nd.nrOfElements; j++) {
				if(less(key, nd.keys[j].key)) {
					// DISK-READ(nd.keys[j].leftChild)
					newChild = insert(nd.keys[j].leftChild, key, ht-1);
					if(newChild == null) 
						return null;
					// split
					sd.key = nd.keys[j].leftChild.keys[MAX/2].key;	// save middle key of node to promote
					if(nd.keys[j].leftChild != null) {
						for(int i = nd.keys[j].leftChild.keys.length-1; i > MAX/2-1; i--) 
							nd.keys[j].leftChild.keys[i] = null; 
					}
					sd.leftChild = nd.keys[j].leftChild;			// connect left child to the new key fixme
					sd.rightChild = newChild;						// connect right child to the new key
					nd.keys[j].leftChild = newChild;				// update next key with newly updated child
					// close disk reader
					break;
				}
				else if(j+1 == nd.nrOfElements) {
					// DISK-READ(nd.keys[j].rightChild)
					newChild = insert(nd.keys[j].rightChild, key, ht-1);
					if(newChild == null) 
						return null;
					// split
					sd.key = nd.keys[j].rightChild.keys[MAX/2].key;	// save middle key of node for promoting
					if(nd.keys[j].rightChild != null) {
						for(int i = nd.keys[j].rightChild.keys.length-1; i > MAX/2-1; i--)
							nd.keys[j].rightChild.keys[i] = null;
					}
					sd.leftChild = nd.keys[j].rightChild;			// unite the child located between the promoted key and the previous key
					sd.rightChild = newChild;
					// close disk reader
					break;
				}
			}
		}
		
		// this if bracket makes sure to insert at leaf level
		if(newChild == null && nd.keys[j] != null && less(key, nd.keys[j].key) && nd.keys[j].leftChild != null) {
			// DISK-READ(nd.keys[j].leftChild)
			insert(nd.keys[j].leftChild, key, ht-1);
			// close disk reader
		}
		else if (newChild == null && nd.keys[j] != null && less(key, nd.keys[j].key) && nd.keys[j].rightChild != null) {
			// DISK-READ(nd.keys[j].rightChild)
			insert(nd.keys[j].rightChild, key, ht-1);
		}
		else {
			for (int i = nd.nrOfElements; i > j; i--)
				nd.keys[i] = nd.keys[i-1];
			if(newChild != null) {	// handles when a key was promoted, incrementing j to put the newly promoted key in the right place
				for(j = 0; j < nd.nrOfElements; j++) {
					if(less(key, nd.keys[j].key))
						break;
				}
			}
			nd.keys[j] = sd;
			nd.nrOfElements++;
	        // DISK-WRITE(nd)
	        // DISK-WRITE(nd.keys[j].leftChild)
	        // DISK-WRITE(nd.keys[j].rightChild)
		}
		
		if(nd.nrOfElements < MAX) {
			// DISK-WRITE(nd)
			// close disk writer
			return null;
		}
		else
			return split(nd);
			
	}
	
	// makes a new node that contains the latter half of the passed in node's keys
	private Node split(Node n) {
		Node temp = new Node(MAX/2);
		n.nrOfElements = MAX/2;
		for(int j = 0; j < MAX/2; j++) 
			temp.keys[j] = n.keys[MAX/2+j+1];
		return temp;
	}
	
	private boolean less(String key1, String key2) {
		return key1.compareTo(key2) < 0;
	}

	// Serialization 
	private static void diskWrite(Node node) {
	       try {
	           FileOutputStream file = new FileOutputStream("tree.ser");
	           ObjectOutputStream out = new ObjectOutputStream(file);
	              
	           // serialize object
	           //out.defaultWriteObject();
	           out.writeObject(node);
	            
	           out.close();
	           file.close();
	       }
	       catch(IOException ex) {
	    	   System.out.println("IOException is caught");
	       }
	}

	// Deserialization
	public static Node diskRead() {
		Node deserializedNode = null;
        try {   
        	FileInputStream file = new FileInputStream("tree.ser");
	        ObjectInputStream in = new ObjectInputStream(file);
	              
	        // deserialize object
	        //in.defaultReadObject();
	        deserializedNode = (Node)in.readObject();
	        
	        in.close();
	        file.close();
	    }
	    catch(IOException ex) {
	    	System.out.println("IOException is caught");
	    }
	    catch(ClassNotFoundException ex) {
	        System.out.println("ClassNotFoundException is caught");
	    }
	        
	    return deserializedNode;
	}
	
	public void getInorder(Node node, int ht) {
		sortedKeys = new String[totalKeys];
		index = 0;
		getInorderHelper(node, ht);
	}
	
	// traverse tree in InOrder fashion, recording all keys (in sortedKeys) along the way.
	private void getInorderHelper(Node node, int ht) {
		if(node == null) 
			return;
		// at leaf - get all keys in the node.
		if(ht == 0) {
			for(int i = 0; i < node.nrOfElements; i++) {
				sortedKeys[index] = node.keys[i].key;
				index++;
			}
		}
		// at internal node,
		else {
			for(int j = 0; j < node.nrOfElements; j++) {		// for all keys in the node,
				// DISK-READ(node.keys[j].leftChild)
				getInorderHelper(node.keys[j].leftChild, ht-1);		// recurse until at left-most leaf,
				sortedKeys[index] = node.keys[j].key;			// record key at i of internal node,
				index++;
				if(j == node.nrOfElements-1) {							// if at last key in the node,
					// DISK-READ(node.keys[j].rightChild)
					getInorderHelper(node.keys[j].rightChild, ht-1);	// recurse for right child.
					// close disk reader
				}
				// close disk reader
			}
		}
	}
	
	/*public static void main(String[] args) throws IOException {
//		BusinessRecommender rec = new BusinessRecommender();
//		String[] inorderKeys = new String[BTree.totalKeys];
//		String[] randomMedoids = new String[5];
//		rec.IDTree.sortedKeys = new String[BTree.totalKeys];
//		
//		getInorder(rec.IDTree.root, rec.IDTree.height);
//		inorderKeys = rec.IDTree.sortedKeys;
//		for(int i = 0; i < 5; i++) {
//			randomMedoids[i] = inorderKeys[(int) (Math.random() * inorderKeys.length)];
//		}
		
		System.out.println();
	}*/
}
