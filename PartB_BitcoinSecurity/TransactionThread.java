import java.security.*;
import java.util.*;

public class TransactionThread implements Runnable{
	public static final boolean DEBUG = false;

	public int nodeId;
	public Thread tId;
	
	public final PublicKey publicKey;
	private final PrivateKey privateKey;
	private boolean isHonest;
	private int startOfDishonests;
	private Vector<DoubleSpend> saveForDoubleSpend;
	private boolean moreDoubleSpend;

	public TransactionThread(int nodeId, PublicKey pk, PrivateKey sk, boolean isHonest, int startOfDishonests){

		this.nodeId = nodeId;
		this.publicKey = pk;
		this.privateKey = sk;
		this.isHonest = isHonest;
		this.startOfDishonests = startOfDishonests;
		this.saveForDoubleSpend = new Vector<>();
		this.moreDoubleSpend = true;
	}
	
	
	public void run(){
		Main.logger.fine("Transaction thread of Node: "+ nodeId +  " started running...");
		//System.out.println("In TransactionThread of node: " + nodeId);
		while(true){
			if(Main.nodes.get(nodeId).bitcoinChain.size() >= Main.stop){
				System.out.println(nodeId + " TxnThread stopping");
			 	break;
			}
		
			try{
				//Random random = new Random();
				Thread.sleep(500);
			}catch(InterruptedException e){
				System.out.println("Sleep Interrupt");
			}
			
			
			if(Main.nodes.get(nodeId).unspentTxns.get(publicKey).size() > 0) {
				if(isHonest){
                    		
            		doTransactions();
            		
        		}else{
        			
        			doTransactionsdisHonest();    			
    			}	
			}
			else{
				//System.out.println("no transactions can be performed");
			}	
		}
	}
	
	public boolean doTransactionsdisHonest(){
		//when transaction phase start just call this function it will randomly create a transaction 
		// will send fraction of money to other nodes
		synchronized(Main.unspentPools.get(nodeId)){
			Random rn = new Random();
			double totalAmount = 0;
			int currentBlockNo = Main.nodes.get(nodeId).bitcoinChain.size(); 
			
			Transaction txn = new Transaction(this.publicKey, nodeId);
			Set<Integer> toDoubleSpend = new HashSet<>();
			int j = 0;
			boolean flag = false;

			HashMap<PublicKey, Vector<UnspentTxn>> unspentTxns = Main.unspentPools.get(nodeId);
			
			Set<String> removedT = new HashSet<>();	
			Vector<UnspentTxn> myUnspent = unspentTxns.get(publicKey);
			int size = myUnspent.size();

			for(int i = 0; i < size && saveForDoubleSpend.size() < 1; i++ ){
				UnspentTxn unstxn = myUnspent.get(i);
				boolean letsCheat = rn.nextBoolean();
				if(letsCheat){
					DoubleSpend ds = new DoubleSpend(unstxn, currentBlockNo);
					saveForDoubleSpend.add(ds);
					System.out.println(nodeId + " Saved for further spend currH: " + 
						currentBlockNo + " index: " + unstxn.indexInOutOfTxn);
				}
			}

			while(!myUnspent.isEmpty()){   //Assuming it will add all unspent txns and will keep the change
				UnspentTxn unstxn = myUnspent.remove(0);
				
				String a = unstxn.txnHash.toString()+String.valueOf(nodeId)+String.valueOf(unstxn.amount);
				if(removedT.contains(a)) continue;
				
				removedT.add(a);
				
				//System.out.println("The hash that was used in txn was: " + unstxn.txnHash + "sender: " + nodeId + " with amount: " + unstxn.amount);
				totalAmount+= unstxn.amount;
				//System.out.println("Amount: "+totalAmount+" in node :"+nodeId+"shown in  txn:"+unstxn.amount);
				txn.addInputToTxn(unstxn.txnHash, unstxn.indexInOutOfTxn);
			}

			j = 0;
			flag = false;

			for(j = 0; j < saveForDoubleSpend.size() && currentBlockNo > 5 ; j++){
				if(saveForDoubleSpend.get(j).numSpent < 2 && currentBlockNo - saveForDoubleSpend.get(j).lastSpent >=2){
					UnspentTxn utxo = saveForDoubleSpend.get(j).utxo;
					totalAmount+= utxo.amount;
					//System.out.println("Amount: "+totalAmount+" in node :"+nodeId+"shown in  txn:"+unstxn.amount);
					txn.addInputToTxn(utxo.txnHash, utxo.indexInOutOfTxn);
					flag = true;
					saveForDoubleSpend.get(j).numSpent++;
					saveForDoubleSpend.get(j).lastSpent = currentBlockNo;
					System.out.println(nodeId + " Created an double spend currH: " + currentBlockNo + " txHash: " 
					+ Main.toHexString(utxo.txnHash).substring(0, 15) + " index: " + utxo.indexInOutOfTxn);
				}
			}

		
		//Main.nodes.get(nodeId).unspentTxns.put(this.publicKey, myUnspent);
		//System.out.println("Amount: "+totalAmount+" in node :"+nodeId);
		
		if(totalAmount > 0){
			int numTransaction =  1+rn.nextInt((int)(Main.numNodes*0.7));
			if(numTransaction > 5) numTransaction = 5;
			  //will return a random int these number of output will be included
			Set<Integer> alreadySent = new HashSet<>();
			
			while(numTransaction > 0 && totalAmount > 1){
				double fractionPay = rn.nextDouble();  //will return uniformely distributed [0,1]
				int randomNodeId = rn.nextInt(Main.numNodes);  //will return a random nodeId
				if(randomNodeId!= this.nodeId && !alreadySent.contains(randomNodeId)){
					double toPay = 0;
					if(totalAmount < Main.oneSatoshi){
						toPay = totalAmount;
					}else{
					 	toPay= fractionPay*totalAmount;
					}
					txn.addOutputToTxn(Main.nodes.get(randomNodeId).getPublickey(), toPay);
					System.out.println("The value of "+toPay+" transferred to "+randomNodeId+" by "+nodeId);
					totalAmount-= toPay;
					alreadySent.add(randomNodeId);
					numTransaction--;
					Main.logger.info("Transaction: " + toPay +  " bitcoins from " + nodeId +  " to " + randomNodeId);

				}
			}
			
			if(totalAmount > 0){
				txn.addOutputToTxn(this.publicKey, totalAmount);
				Main.logger.info("Transaction: " + totalAmount + " bitcoins from " + nodeId +  " to self");

			}
			Main.nodes.get(nodeId).signTransaction(txn);
			
			//System.out.println(nodeId + " created transaction: "+ Main.toHexString(txn.txHash).substring(0, 10));

			broadCast(txn);
			//Main.nodes.get(nodeId).validTransactions.add(txn);
			return true;

		}else{
			return false;
		}
		}
	}
	public boolean doTransactions(){
		//when transaction phase start just call this function it will randomly create a transaction 
		// will send fraction of money to other nodes
		synchronized(Main.unspentPools.get(nodeId)){
			Random rn = new Random();
			double totalAmount = 0;
			Transaction txn = new Transaction(this.publicKey, nodeId);

			HashMap<PublicKey, Vector<UnspentTxn>> unspentTxns = Main.unspentPools.get(nodeId);
			
			Set<String> removedT = new HashSet<>();		
			Vector<UnspentTxn> myUnspent = unspentTxns.get(publicKey);
		
			//System.out.println("Unspent txns in node :"	+	nodeId 	+	"is "	+	myUnspent.size());
		
			while(!myUnspent.isEmpty()){   //Assuming it will add all unspent txns and will keep the change
				UnspentTxn unstxn = myUnspent.remove(0);
				
				String a = unstxn.txnHash.toString()+String.valueOf(nodeId)+String.valueOf(unstxn.amount);
				if(removedT.contains(a)) continue;
				
				removedT.add(a);
				
				//System.out.println("The hash that was used in txn was: " + unstxn.txnHash 	+ "node ID: " + nodeId + " with amount: " + unstxn.amount);
				totalAmount+= unstxn.amount;
				//System.out.println("Amount: "+totalAmount+" in node :"+nodeId+"shown in  txn:"+unstxn.amount);
				txn.addInputToTxn(unstxn.txnHash, unstxn.indexInOutOfTxn);
			}
			
			if(totalAmount > 0){
				int numTransaction =  1+rn.nextInt((int)(Main.numNodes*0.7));
				if(numTransaction > 5) numTransaction = 5;
				  //will return a random int these number of output will be included
				Set<Integer> alreadySent = new HashSet<>();
				
				while(numTransaction > 0 && totalAmount > 1){
					double fractionPay = rn.nextDouble();  //will return uniformely distributed [0,1]
					int randomNodeId = rn.nextInt(Main.numNodes);  //will return a random nodeId
					if(randomNodeId!= this.nodeId && !alreadySent.contains(randomNodeId)){
						double toPay = 0;
						if(totalAmount < Main.oneSatoshi){
							toPay = totalAmount;
						}else{
						 	toPay= fractionPay*totalAmount;
						}
						txn.addOutputToTxn(Main.nodes.get(randomNodeId).getPublickey(), toPay);
						System.out.println("The value of "+toPay+" transferred to "+randomNodeId+" by "+nodeId);
						totalAmount-= toPay;
						alreadySent.add(randomNodeId);
						numTransaction--;
						Main.logger.info("Transaction: " + toPay +  " bitcoins from " + nodeId +  " to " + randomNodeId);
					}
				}
			
				if(totalAmount > 0){
					txn.addOutputToTxn(this.publicKey, totalAmount);
					Main.logger.info("Transaction: " + totalAmount + " bitcoins from " + nodeId +  " to self");
				}

				Main.nodes.get(nodeId).signTransaction(txn);
				
				//System.out.println(nodeId + " created transaction: "+ Main.toHexString(txn.txHash).substring(0, 10));

				broadCast(txn);
				//Main.nodes.get(nodeId).validTransactions.add(txn);
				return true;
			}else{
				return false;
			}
		}
	}
	
 
    public void broadCast(Transaction txn){
    	Message txnMsg = null;
    	synchronized(Main.messagePassingQ){
    		for(int i =0; i < Main.numNodes; i++){
	  			// if(i != nodeId){
	  				txnMsg = new TransactionMessage(3, i, nodeId, txn);
	  				Queue<Message> txnq = Main.messagePassingQ.get(i);
	  				txnq.add(txnMsg);
	  			//}
  			}
    	}
  		
  	}
	
	
	public void start(){
		if(DEBUG)
		System.out.println("Starting Transaction Thread: " + nodeId);
		if(tId == null){
			tId = new Thread(this);
			tId.start();
		}	
	}
}

class DoubleSpend{
	public UnspentTxn utxo;
	public int height;
	public int lastSpent;  //this will not be exact but give rough idea in which block it may cluded.
	public int numSpent;
	public DoubleSpend(UnspentTxn utxo, int height){
		this.utxo = utxo;
		this.height = height;
		this.lastSpent = lastSpent;
		this.numSpent = 1;
	}
}
