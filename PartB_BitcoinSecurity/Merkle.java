import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Math;

class Merkle{
    //private String rootHash;
    public byte[] rootHash;
    private int arity;
    private Vector<byte[]> transactionsHashList;
    private Vector<Vector<byte[]>> tree;


    public Merkle(Vector<Transaction> vec){
        this.arity = Main.arity;
        this.tree = new Vector<>();
        transactionsHashList = new Vector<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        for(int i = 0; i < vec.size(); i+=Main.arity){
        	try{
        	for(int j = i; j < Main.arity && j < vec.size() ; j++){
        	 	byte[] hashTemp = vec.get(j).getHash();
        	 	outputStream.write(hashTemp);
        	 }
        	
        	transactionsHashList.add(Crypto.sha256(outputStream.toByteArray()));
        	
        	outputStream.reset();
        	}catch(IOException e){
              System.out.println("Exception merkle");
            }
            
        }

        transactionsHashList.add(null);
        
        byte[] tempHash;
        double size = transactionsHashList.size();
        
        Vector<byte[]> temp = new Vector<>();
        
        while(transactionsHashList.size() > 2){
        	
        	
        	try{
        	for(int i = 0; i < Main.arity && transactionsHashList.get(0) != null; i++){ 
            outputStream.write(transactionsHashList.remove(0));
        	
        	}
        	tempHash = outputStream.toByteArray();
        	transactionsHashList.add(Crypto.sha256(tempHash));
        	outputStream.reset();
        	
        	if(transactionsHashList.get(0) == null){ 
        		transactionsHashList.remove(0);
        		transactionsHashList.add(null);
        	}     	
        	}catch(IOException e){
              System.out.println("Exception merkle");
            }
        }
        
        rootHash = transactionsHashList.remove(0);
    }


    public byte[] getMerkleRootHash(){
      return rootHash;
    }

}


  