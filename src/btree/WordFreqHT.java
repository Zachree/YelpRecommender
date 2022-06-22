package btree;
/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author zachf
 */
public class WordFreqHT implements Serializable {
    
    static class Node implements Serializable {
        final Object key;
        Object val;
        double num = 0;
        Node(Object k) { 
            key = k;
            num++;
        }
        Node(Object k, Object v){
            key = k;
            val = v;
            num++;
        }
        Node(Object k, Object v, double f){
            key = k;
            val = v;
            num = f;
        }
    }
    
    Node[] table = new Node[8];
    int count;
    
    double getCount(Object key){
        int h = MurmurHash2.hash32(key.toString());
        int i = h & (table.length-1);
        /*for(Node e = table[i]; e != null; e = e.next) {
            if(key.equals(e.key))
                return e.val;
            else{
                i = LinearProbe(i, key);
                return table[i].key;
            }
        }*/
        if(table[i] != null){               // if slot has something,
            if(key.equals(table[i].key))    // check if it's actually the key we're looking for
                return table[i].num;
            else                           // or if not the key we're looking for,
                i = LinearProbe(i, key);   // linear probe whole table for matching key (because of open address approach), but if none, return next open slot.
        }
        if(table[i] == null)               // if LinearProbe returned an empty slot,
            return 0;
        else                              // or if LinearProbe returned a slot with a matching key.
            return table[i].num;
    }
    
    Object get(Object key){
        int h = MurmurHash2.hash32(key.toString());
        int i = h & (table.length-1);
        /*for(Node e = table[i]; e != null; e = e.next) {
            if(key.equals(e.key))
                return e.val;
            else{
                i = LinearProbe(i, key);
                return table[i].key;
            }
        }*/
        if(table[i] != null){               // if slot has something,
            if(key.equals(table[i].key))    // check if it's actually the key we're looking for
                return table[i].val;
            else                           // or if not the key we're looking for,
                i = LinearProbe(i, key);   // linear probe whole table for matching key (because of open address approach), but if none, return next open slot.
        }
        if(table[i] == null)               // if LinearProbe returned an empty slot,
            return 0;
        else                              // or if LinearProbe returned a slot with a matching key.
            return table[i].val;
    }
    
    void put(Object key) {
        int h = MurmurHash2.hash32(key.toString());
        int i = h & (table.length-1);
        /*for(Node e = table[i]; e != null; e = e.next){
            if(key.equals(e.key)){
                e.val += val;
                return;
            }
        }*/
        if(table[i] != null){               // if slot is not empty,
            if(key.equals(table[i].key)){   // and is the key the word we're adding,
                table[i].num++;             // increment the frequency of that word.
                return;
            }
            else                           // or if not same key,
                i = LinearProbe(i, key);   // linear probe whole table for matching key, but if none, return next open slot.
        }
        if(table[i] == null)               // if LinearProbe returned an empty slot,
            table[i] = new Node(key);      // create a new entry
        else{                              // or if LinearProbe returned a slot with a matching key.
            table[i].num++;
            return;
        }
        ++count;
        if(((double) count/(double) table.length) > 0.75){ 
            resize();
        }
    }
    
    void put(Object key, Object val) {
        int h = MurmurHash2.hash32(key.toString());
        int i = h & (table.length-1);
        /*for(Node e = table[i]; e != null; e = e.next){
            if(key.equals(e.key)){
                e.val += val;
                return;
            }
        }*/
        if(table[i] != null){               // if slot is not empty,
            if(key.equals(table[i].key)){   // and is the key the word we're adding,
                if(!table[i].val.toString().contains(val.toString()))  // only add the business ID to the val if it isn't already there.
                    table[i].val += " " + val;
                table[i].num++;             // increment the frequency of that word.
                return;
            }
            else                           // or if not same key,
                i = LinearProbe(i, key);   // linear probe whole table for matching key, but if none, return next open slot.
        }
        if(table[i] == null)               // if LinearProbe returned an empty slot,
            table[i] = new Node(key, val);      // create a new entry
        else{                              // or if LinearProbe returned a slot with a matching key.
            if(!table[i].val.toString().contains(val.toString()))  // only add the business ID to the val if it isn't already there.
                table[i].val += " " + val;
            table[i].num++;
            return;
        }
        ++count;
        if(((double) count/(double) table.length) > 0.75){ 
            resize();
        }
    }
    
    void resize() {
        Node[] oldTable = table;
        Node[] newTable = new Node[oldTable.length * 2];
//        for(int i = 0; i < table.length; ++i) {
//            for(Node e = oldTable[i]; e != null; e = e.next){
//                int h = MurmurHash2.hash32(e.key.toString());
//                int j = h & (newTable.length-1);
//                newTable[j] = new Node(e.key, e.val);
//            }
//        }
        for(int i = 0; i < table.length; ++i){
            if(oldTable[i] != null){
                int h = MurmurHash2.hash32(oldTable[i].key.toString());
                int j = h & (newTable.length-1);
                if(newTable[j] != null)    // if collision, linearly probe for next open slot.
                    j = nextEmptySlot(j, newTable);
                newTable[j] = new Node(oldTable[i].key, oldTable[i].val, oldTable[i].num);
            }
        }
        table = newTable;
    }
    
    void printAll() {
        int k = 0;
//        for(int i = 0; i < table.length; ++i) {
//            for(Node e = table[i]; e != null; e = e.next){
//                System.out.println(k + " Key: " + e.key + ", Val: " + e.val);
//                k++;
//            }
//        }
        for(int i = 0; i < table.length; ++i) {
            if(table[i] != null){
                System.out.println(k + " Key: " + table[i].key+ ", Freq: " + table[i].num + ", Val: " + table[i].val);
                k++;
            }
       }
    }
    
        private int nextEmptySlot(int i) {
        int index = i;
        while(index < table.length){    // look to the right of i for an open slot
            if(table[index] == null)
                return index;
            index++;
        }
        if(index >= table.length)   // if the previous loop reached the end of the table,
            index = 0;              // wrap around to the front, and then
        while(index < i){           // look from all the way to the left until reaching i or an empty slot.
            if(table[index] == null)
                return index;
            index++;
        }
        return i;
    }
    
    private int nextEmptySlot(int i, Node[] newTable) {
        int index = i;
        while(index < newTable.length){    // look to the right of i for an open slot
            if(newTable[index] == null)
                return index;
            index++;
        }
        if(index >= table.length)   // if the previous loop reached the end of the table,
            index = 0;              // wrap around to the front, and then
        while(index < i){           // look from all the way to the left until reaching i or an empty slot.
            if(newTable[index] == null)
                return index;
            index++;
        }
        return i;
    }
    
    private int LinearProbe(int i, Object key){    // desired key not found at i. probe linearly until the key is found, but if it does not exist, return null.
        int index = i;
        while(index < table.length && table[index] != null){    // look to the right of i for the desired key or next empty slot, whichever comes first.
            if(key.equals(table[index].key))
                return index;
            index++;
        }
        if(index >= table.length)                   // if the previous loop reached the end of the table,
            index = 0;                              // wrap back around to the front,
        while(index < i && table[index] != null){   // and look from all the way to the left until reaching either i, an empty slot,
            if(key.equals(table[index].key))        // or the desired key - whichever is found first.
                return index;
            index++;
        }
        i = nextEmptySlot(i);   // reaching this point means no matching key was found, so then locate the next empty slot
        return i;
    }

	// Serialization
	public static void diskWrite(Node[] table) {
	       try {
	           FileOutputStream file = new FileOutputStream("wordfreqht.ser");
	           ObjectOutputStream out = new ObjectOutputStream(file);
	           
	           // serialize object
	           //out.defaultWriteObject();
	           out.writeObject(table);
	            
	           out.close();
	           file.close();
	       }
	       catch(IOException ex) {
	    	   System.out.println("IOException is caught");
	       }
	}
}
