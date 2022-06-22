package btree.tests;

import java.io.*;

public class SerializeTest {
	
	private static class Node implements Serializable {
		int num;
		subNode[] nodes;
		private Node(int n) {
			num = n;
		}
	}
	private static class subNode implements Serializable {
		String key;
		private subNode(String k) {
			key = k;
		}
	}

	public static void main(String[] args) {

		Node node1 = new Node(50);
		Node node2 = new Node(100);
		String[] strings = {"gay", "yag", "yig" };
		node1.nodes = new subNode[strings.length];
		node2.nodes = new subNode[strings.length];
		for(int i = 0; i < strings.length; i++)
			node1.nodes[i] = new subNode(strings[i]);
		int index = 0;
		for(int j = strings.length-1; j >= 0; j--) {
			node2.nodes[index] = new subNode(strings[j]);
			index++;
		}
		diskWrite(node1);
		diskWrite(node2);
        Node deserializedNode2 = diskRead();
        System.out.println("Object has been deserialized ");
        System.out.println("Node number = " + deserializedNode2.num);
        for(int i = 0; i < deserializedNode2.nodes.length; i++) {
            System.out.println("sub Node's strings = " + deserializedNode2.nodes[i].key);
        }
        
		System.out.println();
	}

	// Serialization 
	private static void diskWrite(Node node) {
        try {
            FileOutputStream file = new FileOutputStream("serializeTestFile.txt");
            ObjectOutputStream out = new ObjectOutputStream(file);
              
            // serialize  object
            out.writeObject(node);
            
            out.close();
            file.close();
        }
        catch(IOException ex) {
            System.out.println("IOException is caught");
        }
	}

    // Deserialization
	private static Node diskRead() {
		Node deserializedNode = null;
        try {   
            FileInputStream file = new FileInputStream("serializeTestFile.txt");
            ObjectInputStream in = new ObjectInputStream(file);
              
            // deserialize object
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

}
