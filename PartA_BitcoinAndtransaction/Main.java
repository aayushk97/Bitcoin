import java.util.Scanner;
import java.math.BigInteger; 
import java.security.*;
import java.util.*;
import java.io.*;
import java.io.IOException;
import java.util.logging.*;
import java.util.concurrent.atomic.*;
import java.lang.instrument.Instrumentation;


public class Main{
	
	public static final double MINING_REWARD = 50;  //Reward amount
	public static int w = Parameters.W;  //first w bits to be zero
    public static volatile int nonceSize = Parameters.NONCESIZE;
    public static int arity = Parameters.ARITY; //default
    public static int maxTransactionInBlock = 10; 
    public static int stop = 20; //least length of blockchain when to stop

    public static boolean printOnConsole = false;  //if want logs to print on console set it to true.
    public static boolean wantToprintTxn = true;    //prints all txns on console when kept true
	public static int numNodes;
	public static Vector<Node> nodes;  
	//min amount of bitcoin that can be transferred different from actual value
	public static double oneSatoshi = 0.005; 						                 
	public static double probToSend = 0.2;
	public static Vector<Queue<Message>> messagePassingQ;  
    public static Logger logger;
    public static String path = "logfiles";
    
    public static Vector<Transaction>invalidTxns ;
    public static HashMap<PublicKey, Integer> publicKeymappings;
    public static Vector<HashMap<PublicKey, Vector<UnspentTxn>> > unspentPools;
    public static Object printingMutex = new Object();
    public static Object unspLock = new Object();
	public static boolean REPORT = false;
    public static AtomicBoolean mined = new AtomicBoolean(false);

	public static void main(String[] args){
		nodes = new Vector<Node>();
	    invalidTxns = new Vector<>();
        publicKeymappings = new HashMap<>();
		Scanner in = new Scanner(System.in);
		unspentPools = new Vector<>();
        messagePassingQ = new Vector<>();
    
		int disHonest = 0;
		
	if(!REPORT){
			System.out.println("Enter the number of nodes in network");		
			numNodes = in.nextInt();

    	}else{
    		numNodes = Integer.parseInt(args[0]);
    		disHonest = Integer.parseInt(args[1]);
    		nonceSize = Integer.parseInt(args[2]);
    		Crypto.hash = args[3];
    		arity = Integer.parseInt(args[4]);
    		w = Integer.parseInt(args[5]);
    	}
        	 
        boolean append = false;
        FileHandler handler = null;

        try{
            handler = new FileHandler("logfiles/MyLogFile.log", append);
            handler.setFormatter(new SimpleFormatter());

        }catch(IOException e){
            System.out.println("Exception");
        }   
	
	
        LogManager lgmngr = LogManager.getLogManager();
        logger = lgmngr.getLogger(Logger.GLOBAL_LOGGER_NAME);
        //logger.setLevel(Level.CONFIG);
        logger.addHandler(handler);
        
        logger.setUseParentHandlers(printOnConsole);

        //start of the algorithm
        long startTime = System.nanoTime();
            	
        for(int i = 0; i < numNodes; i++){
            messagePassingQ.add(new LinkedList<>());
            unspentPools.add(new HashMap<>());
        }

    	//initialize each node
    	for(int i = 0 ; i < numNodes; i++ ){
        		nodes.add(new Node(i));
    	}

    	//initialize bitcoin chain in all nodes
    	for(int i = 0; i < numNodes; i++){
    		nodes.get(i).initializeBitcoinChain();

    	}
    	
    	Main.nodes.get(0).mineGenesisBlock();

	   //Start the threads	
    	for(int i = 0 ; i < numNodes; i++ ){
            	nodes.get(i).start();
    	}

    	for(int i =0; i < numNodes; i++){
    		try{
    			nodes.get(i).txnThreadNode.tId.join();
    			nodes.get(i).tId.join();
    			
    		}catch(InterruptedException e){
    			System.out.println("InterruptedException");
    		}		
        }
		
		int longestChainSize = stop;
	
        //Transactions performed while creating bitcoin chain
        if(wantToprintTxn){
            System.out.println("The transactions that were performed: ");
            TransactionsPerformed(nodes.get(0).bitcoinChain);
            System.out.println();
        }

        
        System.out.println("Final States of nodes: \n");

        for(int j = 0; j < nodes.size(); j++){
            int size = nodes.get(j).bitcoinChain.size();
            if(size > longestChainSize){
                longestChainSize = size;
            }
            System.out.println("Node: " + nodes.get(j).nodeId);
			for(int i = 0; i < size; i++){
                Block blk = nodes.get(j).bitcoinChain.get(i);

				System.out.println("Block hash: "
                    + toHexString(blk.blockHash) + " No of Txns: "+ blk.transactionsInBlock.size()
                    + " height: "+ blk.height);	
		
			}
			System.out.println("");
		}

        long endTime = System.nanoTime();
        
        //test Correctness of blockchain
        //System.out.println("\nLongest chain: " + longestChainSize);
        for(int i = 0 ; i < longestChainSize; i++){
            for(int j = 1 ; j < numNodes; j++){
                String h1 = null;
                String h2 = null;
                if(i < nodes.get(j).bitcoinChain.size()){
                    h1 = toHexString(nodes.get(j).bitcoinChain.get(i).blockHash);
                }
                if(i < nodes.get(j - 1).bitcoinChain.size()){
                    h2 = toHexString(nodes.get(j - 1 ).bitcoinChain.get(i).blockHash);
                }
                if(h1 != null && h2 !=null){
                    if(!h1.equals(h2)){
                        System.out.println("Mismatch blockNo: " + i + " nodes " + (j -1) + " and " + j);
                    }
                }
                // else if (h1 != null){
                //     System.out.println("Node " + j + " greater");
                // }else if (h2 != null){
                //     System.out.println("Node " + (j -1) + " greater");
                // }
            }
        }

        for(int i = 0; i < numNodes; i++ ){
            System.out.println("Node Id " + i);
            doesItContainDoubleSpend(nodes.get(i).bitcoinChain);
            System.out.println();
        }
        
        //find the size implications
        // int size1 = 0;
        // int txnNum = 0;
        // for(int i = 0; i < nodes.get(0).bitcoinChain.size() ; i++){
        // 	Block blk = nodes.get(0).bitcoinChain.get(i);
        // 	txnNum += blk.transactionsInBlock.size();
        // 	System.out.println("Number of transactions untill now: "+txnNum);
        // 	size1 +=  blk.getSize();
        // 	System.out.println("Size upto now: "+size1);
        	
        // }
        
        // System.out.println("Space required for bitcoin chain: "+size1+" bytes");
        
        // int size2 = 0;
        
        // for(int i = 0; i < numNodes; i++){
        // 	size2 +=  getSize(nodes.get(i));
        // }
        
        // System.out.println("Overall space required for "+numNodes+" nodes: "+size2+" bytes");
        
        
        // long duration = (endTime - startTime);
        
        // System.out.println("Time taken to execute: "+(float)(duration)/1000);

        checkifBlockchainValid(nodes.get(0).bitcoinChain);
        //createData();
    }

    public static void doesItContainDoubleSpend(Vector<Block> bitcoinChain){
        HashMap<String, Spends> trackSpends = new HashMap<>();
        boolean flag = true;
        for(int i =0; i < bitcoinChain.size(); i++){
        	
            Block blk = bitcoinChain.get(i);
            Vector<Transaction> txns = blk.transactionsInBlock;
            
            for(int k =0; k < txns.size(); k++){
                
                Transaction txn = txns.get(k);
                Vector<Input> inputs = txn.inputTxns;
                int senderId = txn.senderId;
                
                for(int j =0; j < inputs.size(); j++){
                    Input inp = inputs.get(j);
                    byte[] refTxn = inp.refTxn;
                    int index = inp.receiptIndex;
                    
                    Spends sp = new Spends(refTxn, senderId, index, i, txn);
                    
                    if(trackSpends.containsKey(sp.id))
                    {	Spends sp2 = trackSpends.get(sp.id);
                        System.out.println("Already exists: previously in: "
                            + sp2.blockNum + " with txnid: "+ toHexString(sp2.txns.txHash).substring(0, 10) 
                            + " now in blKNo: " + sp.blockNum 
                            + " and txn hash: "+ toHexString(sp.txns.txHash).substring(0, 10)+" the ref txn is :"
                            + toHexString(refTxn ).substring(0, 10) + " pre ref: "
                            + sp2.txnHash.substring(0, 10) + " id: " + (sp.id).substring(0, 10) 
                            + " senderid: "+ senderId + " index: " + index
                            + " senderid other txn: " + sp2.senderId);

                        trackSpends.get(sp.id).count+=1;
                        flag = false;
                    }else{
                        trackSpends.put(sp.id, sp);
                    }
                }
            }    
        }
        if(flag)
            System.out.println("No doublespend or repeated Txn in this node's blockchain");
    }
    
    public static void TransactionsPerformed(Vector<Block> bitcoinChain){
    	for(int i = 0; i < bitcoinChain.size(); i++){
    		Block blk = bitcoinChain.get(i);
        	Vector<Transaction> txns = blk.transactionsInBlock;
            System.out.println("Block No: " + blk.height);
            System.out.println("Coinbase: Amount 50.0 paid to miner " + blk.minerId); 
        	for(int k =0; k < txns.size(); k++){
           	
            	Transaction txn = txns.get(k);
           	
           	    //System.out.println("The time taken for transaction in: "+blk.height+" is: "
                //+(blk.timeStampInserted - txn.transactionCreated));
               	
                for(int j = 1; j < txn.outputTxns.size(); j++){
               		System.out.println("Transfered: "+txn.outputTxns.get(j).amount+ " from Node: "
                        + publicKeymappings.get(txn.sender)+" to: "+publicKeymappings.get(txn.outputTxns.get(j).receiver));
               	}
                System.out.println();	     	 
            }            
    	}
    	
    
    
    }

    public static boolean checkifBlockchainValid(Vector<Block> chain){
        for(int i = 1; i < chain.size(); i++){
            if(!Arrays.equals(chain.get(i - 1).blockHash, chain.get(i).prevBlockHash)){

                System.out.println("blockchain invalid");
        
                return false;
            }
        }
        System.out.println("blockchain valid");
        return true;
    }

    public static void createData(){
        // String directory = System.getProperty("user.home");
        // System.out.println("directory " + directory);
        String fileName = "data.txt";
        //String absolutePath = directory + File.separator + fileName;

        // Write the content in file 
        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            Vector<Block> chain =  nodes.get(0).bitcoinChain;
            String fileContent = "";
            for(int i = 0; i < chain.size(); i++){
                Block blk = chain.get(i);
                fileContent = toHexString(blk.blockHash) + "\t" + String.valueOf(blk.transactionsInBlock.size()) 
                + "\t" + String.valueOf(blk.timeTakentomine) +"\n";
                bufferedWriter.write(fileContent);
            }
            
        } catch (IOException e) {
            // Exception handling
        }
    }
    public static long getSize(Object object) {
         return ObjectSizeFetcher.getObjectSize(object);
    }
    public static void printObjectSize(Object object) {
        System.out.println("Object type: " + object.getClass() +
          ", size: " + ObjectSizeFetcher.getObjectSize(object) + " bytes");
    }

	public static String toHexString(byte[] b)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++)
        {
            sb.append(String.format("%02X", b[i] & 0xFF));
        }
        return sb.toString();
    }
    
    public static byte[] getBytesFromDouble(Double value) {
        byte[] bytes = new byte[Double.BYTES];
        Long x = Double.doubleToLongBits(value);
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte)((x >> ((7 - i) * 8)) & 0xff);
        }
        return bytes;
    }
}

class Spends{
    public String id;
    public String txnHash;
    public int senderId;
    public int index;
    public int count;
    public int blockNum;
    public Transaction txns;
    public Spends(byte[] txnHash, int senderId, int index, int blockNum, Transaction txn){
        this.txnHash = Main.toHexString(txnHash);
        this.senderId = senderId;
        this.index = index;
        this.count = 1;
        this.blockNum = blockNum;
        this.txns = txn;
        this.id = this.txnHash + String.valueOf(senderId) + String.valueOf(index);
    }
    public void increaseCount(){ this.count++; }
    public boolean isEqual(Spends spend){
        if(this.txnHash.equals(spend.txnHash) && this.senderId == spend.senderId && this.index == spend.index){
            return true;
        }
        return false;
    }
}
