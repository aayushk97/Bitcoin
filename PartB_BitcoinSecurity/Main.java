import java.util.Scanner;
import java.math.BigInteger; 
import java.security.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.*;


public class Main{
	
	public static final double MINING_REWARD = 50;  //Reward amount
	public static int w = 15;  //first w bits to be zero
	public static int numNodes;
	public static Vector<Node> nodes;
    public static volatile int nonceSize = 20;
	public static int arity = 2; //default
	public static double oneSatoshi = 0.005; //min amount of bitcoin that can be transferred
						                      //different from actual value
	public static int stop = 20;
	public static int maxTransactionInBlock = 20;   
	public static double probToSend = 0.2;
	
	static Vector<Queue<Block>> blockReceivingQueues;
	static Vector<Queue<Transaction>> txnReceivingQueues;
	static Vector<Queue<Message>> messagePassingQ;  
	
	public static boolean REPORT = false;
    	static Logger logger;
    	static Logger txnLogger;
    	static String path = "logfiles";
    	static boolean printOnConsole = false;  //if want logs to print on console set it to true.
    	static Vector<Transaction>invalidTxns ;
    	static HashMap<PublicKey, Integer> publicKeymappings;
    	static Vector<HashMap<PublicKey, Vector<UnspentTxn>> > unspentPools;
    	static Object printingMutex = new Object();
    	static Object unspLock = new Object();

	public static void main(String[] args){
		nodes = new Vector<Node>();
	    invalidTxns = new Vector<>();
        publicKeymappings = new HashMap<>();
		Scanner in = new Scanner(System.in);
		unspentPools = new Vector<>();
	int disHonest = 0;
	
	if(args.length == 2 && REPORT){	
		numNodes = Integer.parseInt(args[0]);
        	
        	System.out.println("Out of this how many you want to be disHonest");
        	
        	disHonest = Integer.parseInt(args[1]);
	}else{
		System.out.println("Enter the number of nodes in network");
		numNodes = in.nextInt();
        	
        	System.out.println("Out of this how many you want to be disHonest");
        	
        	disHonest = in.nextInt();
		
	
	}
        
        boolean append = false;
        FileHandler handler = null;
        //FileHandler handler2 = null;
        try{
            handler = new FileHandler("logfiles/MyLogFile.log", append);
            handler.setFormatter(new SimpleFormatter());
            // handler2 = new FileHandler("logfiles/TxnLog.log", append);
            // handler2.setFormatter(new SimpleFormatter());

        }catch(IOException e){
            System.out.println("Exception");
        }   

        LogManager lgmngr = LogManager.getLogManager();
        logger = lgmngr.getLogger(Logger.GLOBAL_LOGGER_NAME);
        //logger.setLevel(Level.CONFIG);
        logger.addHandler(handler);
        
        logger.setUseParentHandlers(printOnConsole);
        logger.info("Set up log");

		//initialize the queues of each node 
		blockReceivingQueues = new Vector<>();
		txnReceivingQueues = new Vector<>();
	    messagePassingQ = new Vector<>();
	    	
		for(int i = 0; i < numNodes; i++){
			blockReceivingQueues.add(new LinkedList<>());
			txnReceivingQueues.add(new LinkedList<>());	
            messagePassingQ.add(new LinkedList<>());
            unspentPools.add(new HashMap<>());
    	}

    	//initialize each node
    	for(int i = 0 ; i < numNodes - disHonest; i++ ){
        		nodes.add(new Node(i));
    	}
    	for(int i = numNodes - disHonest; i < numNodes; i++ ){
                nodes.add(new Node(i, numNodes - disHonest));
        }
    	//initialize bitcoin chain in all nodes
    	for(int i = 0; i < numNodes; i++){
    		nodes.get(i).initializeBitcoinChain();

    	}
    	
    	Main.nodes.get(0).mineGenesisBlock();
    	
    	
    	for(int i = 0 ; i < numNodes; i++ ){
            	nodes.get(i).start();
    	}

    	for(int i =0; i < numNodes; i++){
    		try{
    			nodes.get(i).mineThreadNode.tId.join();
    			nodes.get(i).txnThreadNode.tId.join();
    			nodes.get(i).tId.join();
    			
    		}catch(InterruptedException e){
    			System.out.println("InterruptedException");
    		}		
	   }
		
		int longestChainSize = stop;

        for(int j = 0; j < nodes.size(); j++){
            int size = nodes.get(j).bitcoinChain.size();
            if(size > longestChainSize){
                longestChainSize = size;
            }
			for(int i = 0; i < size; i++){
				System.out.println("Node: " + nodes.get(j).nodeId+" block hash: "
                    +toHexString(nodes.get(j).bitcoinChain.get(i).blockHash) 
                    + " num of transactions "+ nodes.get(j).bitcoinChain.get(i).transactionsInBlock.size()
                    + " height: "+ nodes.get(j).bitcoinChain.get(i).height);	
		
			}
			System.out.println("");
		}


        //test Correctness of blockchain
        System.out.println("\nLongest chain: " + longestChainSize);
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
                }else if (h1 != null){
                    System.out.println("Node " + j + " greater");
                }else if (h2 != null){
                    System.out.println("Node " + (j -1) + " greater");
                }
            }
        }

   /*     System.out.println("Invalid size: " + invalidTxns.size());
        for(int i = 0; i < numNodes; i++ ){
            
            Set<byte[]> txnSet = nodes.get(i).transactionsInBlockChain;
            for(int j = 0; j < invalidTxns.size(); j++){
                Transaction txn = invalidTxns.get(j);
                if(txnSet.contains(txn.getHash())){
                    System.out.println(i + ": Yes exist");
                }
            }
        }
*/
	
        for(int i = 0; i < numNodes; i++ ){
            System.out.println("Node Id " + i);
            boolean doubleSpend = doesItContainDoubleSpend(nodes.get(i).bitcoinChain);
            if(REPORT && doubleSpend){
             	System.out.println("double in :"+numNodes+" and num MaliciousNodes"+disHonest+"exists: Yes");
             	return;
             } 
            else if(REPORT) {
            	System.out.println("double in :"+numNodes+" and num MaliciousNodes"+disHonest+"exists: No");
            	return;
            }
            System.out.println();
        }
        
        
        for(int i = 0; i < numNodes; i++ ){
            System.out.println("Node Id " + i);
            doesItContainSeen(nodes.get(i).bitcoinChain);
            System.out.println();
        }
        
        
}

	public static void doesItContainSeen(Vector<Block>bitcoinChain){
		Set<byte[]> trackTransactions = new HashSet<byte[]>();	
		for(int i = 0; i < bitcoinChain.size(); i++){
			Block blk = bitcoinChain.get(i);
			
			for(int j = 0; j < blk.transactionsInBlock.size(); j++){
				
				
				if(trackTransactions.contains(blk.transactionsInBlock.get(j).txHash)){
					System.out.println("Dupkicate transactions!");
				}else {
					trackTransactions.add(blk.transactionsInBlock.get(j).txHash);
					
				}
			
			}
		
		}
	
	}
	
    public static boolean doesItContainDoubleSpend(Vector<Block> bitcoinChain){
        HashMap<String, Spends> trackSpends = new HashMap<>();
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
                    	if(sp.blockNum != sp2.blockNum && sp.blockNum != stop){
                    	
                        System.out.println("Already exists: previously in: "
                            + sp2.blockNum + " with txnid: "+ toHexString(sp2.txns.txHash).substring(0, 10) 
                            + " now in blKNo: " + sp.blockNum 
                            + " and txn hash: "+ toHexString(sp.txns.txHash).substring(0, 10)+" the ref txn is :"
                            + refTxn + " pre ref: "
                            + sp2.txnHash.substring(0, 10) + " id: " + (sp.id).substring(0, 10) 
                            + " senderid: "+ senderId + " index: " + index
                            + " senderid other txn: " + sp2.senderId);
                        trackSpends.get(sp.id).count+=1;}
                    }else{
                        trackSpends.put(sp.id, sp);
                    }
                }
            }    
        }
        return false;
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

    public static boolean isFirstwbitsZero(byte[] hash){
        //There was some problem in last isFirstwbitsZero..this is working fine
        int fullBytes = w/8;
        int r = w%8;
        for(int i = 0 ; i < fullBytes; i++){
            if(hash[i] != 0){
                return false;
            }
        }
        //aByte & 0xff
        switch(r){
            case 0:
                return true;
            case 1:
                if(hash[fullBytes] <= 0x7f){
                    return true;
                }else{
                    return false;
                }
            case 2:
                if((hash[fullBytes] & 0xff) <= 0x3f){
                    //System.out.println(hash[fullBytes]);
                    return true;
                }else{
                    return false;
                }
            case 3:
                //System.out.println("3rd?");
                if((hash[fullBytes] & 0xff) <= 0x1f){
                    //System.out.println(hash[fullBytes]);
                    return true;
                }else{
                    return false;
                }
            case 4:
                if((hash[fullBytes] & 0xff) <= 0x0f){
                    //System.out.println(hash[fullBytes]);
                    return true;
                }else{
                    return false;
                }
            case 5:
                //System.out.println("5th?");
                if((hash[fullBytes] & 0xff) <= 0x07){
                    //System.out.println(hash[fullBytes]);
                    return true;
                }else{
                    return false;
                }
            case 6:
                if((hash[fullBytes] & 0xff) <= 0x03){
                    System.out.println(hash[fullBytes]);
                    return true;
                }else{
                    return false;
                }
            case 7:
                if((hash[fullBytes] & 0xff) <= 0x01){
                    //System.out.println(hash[fullBytes]);
                    return true;
                }else{
                    return false;
                }
            default:
                System.out.println("It should never come here");
                return false;
        }
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
