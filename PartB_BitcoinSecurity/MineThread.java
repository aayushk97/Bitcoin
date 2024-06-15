import java.security.*;
import java.util.*;
import java.math.BigInteger;
import java.io.IOException;
import java.io.ByteArrayOutputStream;


public class MineThread implements Runnable{
	static final BigInteger ONE = new BigInteger("1");
	public static final boolean DEBUG = false;
	public int nodeId;
	
	public final PublicKey publicKey;
	private final PrivateKey privateKey;
	
	private int iheight = 1;
	private boolean isHonest;
	private int startOfDishonests;
	
	public Thread tId;

	public MineThread(int nodeId, PublicKey pk, PrivateKey sk, boolean isHonest, int startOfDishonests){
		this.nodeId = nodeId;
		this.publicKey = pk;
		this.privateKey = sk;
		this.isHonest = isHonest;
		this.startOfDishonests = startOfDishonests;
	}
	
	public void run(){
		//System.out.println("MineThread: " + nodeId);
		if(DEBUG)
		Main.logger.fine("Minor thread of Node: "+ nodeId +  " started running...");
		
		while(true){
			if(Main.nodes.get(nodeId).bitcoinChain.size() >= Main.stop) {
				System.out.println(nodeId + " MinerThread stopping");
				break;
			}
			try{
				//after minning each block sleep for 500 secs then mine next block
				Thread.sleep(1000);
			}catch(InterruptedException e){
				System.out.println("Sleep Interrupt");
			}		
			if(Main.nodes.get(nodeId).validTransactions.size() == 0){
				if(DEBUG)
				System.out.println("No transactions valid : "+nodeId);
			}
				
			if(Main.nodes.get(nodeId).validTransactions.size() > 0){			
				if(DEBUG)
				System.out.println("Block minning started by : "+ nodeId);			
				mineBlock();		
			}				
		}	
	}
	
	public void mineBlock(){
		byte[] prevHash = Main.nodes.get(nodeId).bitcoinChain.get(Main.nodes.get(nodeId).bitcoinChain.size()-1).blockHash;
		int height = Main.nodes.get(nodeId).bitcoinChain.size();
		int size = Main.nodes.get(nodeId).validTransactions.size();
		Main.nodes.get(nodeId).minedFirst.set(true);
		
		if(size < 1){
			if(DEBUG)
			System.out.println("No transactions to put in block. Cannot mine an empty block!");
			return;
		} 
		
		//we validate all the transactions
		Transaction txn = null;
		Vector<Transaction> vec = new Vector<>();
		
		//create a coinbase transaction
		Transaction coinBase = Main.nodes.get(nodeId).createCoinbaseTxn();
		Main.nodes.get(nodeId).signTransaction(coinBase);
		  // coinbase txn should be first transaction in the block.
		System.out.println("Hash for coinbase txn was:"+coinBase.txHash+" in node id: "+nodeId);
  				 	
		vec.add(coinBase);	
										// 
		//synchronized(Main.nodes.get(nodeId).validTransactions){
		//System.out.println("Value of size : "+size);
		// for(int i = 0; i < size; i++){
		while(!Main.nodes.get(nodeId).validTransactions.isEmpty()){
			txn = Main.nodes.get(nodeId).validTransactions.remove(0);
			if(Main.nodes.get(nodeId).transactionsInBlockChain.contains(txn.txHash)) continue;
			vec.add(txn);
		}
		//if(vec.size() < 2) return;
		Vector<Transaction> sentToMine = new Vector<>();
		for(int i = 0; i < vec.size(); i++) sentToMine.add(vec.get(i));
		
		Block blk = new Block(prevHash, sentToMine, nodeId, height+1);
		
		//then calculate proof of work
		boolean mined = POW(blk);
		if(mined) {
			broadCast(blk);
		}else {
			//System.out.println("It was mined by other block");
			Block blkInserted = Main.nodes.get(nodeId).bitcoinChain.get(height);
			
			for(int i = 0; i < blkInserted.transactionsInBlock.size(); i++) {
				boolean found = false;	
				for(int j = 0; j < vec.size(); j++) {
						if(Arrays.equals(vec.get(j).txHash, blkInserted.transactionsInBlock.get(i).txHash) )
							found = true;
				}
				if(found == false) Main.nodes.get(nodeId).validTransactions.add(blkInserted.transactionsInBlock.get(i)); 
				
			}
			return;
			
		}
	}
	


	public void start(){
		
		System.out.println("Starting Mine Thread: " + nodeId);
		if(tId == null){
			tId = new Thread(this);
			tId.start();
		}
	}

	public boolean POW(Block blk){ //while mining it needs to keep checking whether next block has arrived
		
  		byte[] s = blk.getBlockInFormOfbytes(); //Should return bytes of prevHash+Txns(merkel root)
  		BigInteger nonce = new BigInteger("0");
  		//System.out.println("Size of the block" + Main.blockReceivingQueues.get(nodeId).size());
  		while(true){
  			//byte[] toBeHashed = concatTwoByteArray(nonce.toByteArray(), s);
  			if(!Main.nodes.get(nodeId).minedFirst.get()) return false;
  			 byte[] nonceBytes = getNonceByte(nonce);   //need to use this function because we have to plot noce size vs time.
  			 byte[] toBeHashed = concatTwoByteArray(nonceBytes, s);

  			try{

  				byte[] hash = Crypto.sha256(toBeHashed);
	            if(isitRequiredhash(hash)){
	            	blk.nonce = nonce;
	            	blk.blockHash = hash;
	            	System.out.println(nodeId + ": Hash: " + Main.toHexString(hash) + " block height: " + blk.height);
	            	blk.addTimeStampM();
	            	//found = true;
	            	break;
	            }
	            
	    		nonce = nonce.add(ONE);
	        }catch(Exception e){
	            System.out.println("Exception");
	        }
	        //System.out.println("Still mining current nonce: " + nonce);
  		}
  		//System.out.println("mining done by "+ nodeId + "!");

  		return true;
  	}

  	public byte[] getNonceByte(BigInteger nonce){
  		byte[] array = nonce.toByteArray();
  		byte[] out = new byte[Main.nonceSize];
  		if(array.length > out.length){
  			System.out.println("Need to increase nonce size");
  			Main.nonceSize += array.length + 5;
  			out = new byte[Main.nonceSize];
  		}
  		int diff = out.length - array.length;
  		for(int i = 0; i < array.length; i++){
  			out[i + diff] = array[i];
  		}
  		return out;
  	}

  	public byte[] concatTwoByteArray(byte[] one, byte[] two){
  		ByteArrayOutputStream combinedBytes = new ByteArrayOutputStream();
  		try{
  			combinedBytes.write(one);
  			combinedBytes.write(two);
  		}catch(IOException e){
  			System.out.println("Exception: in concatTwoByteArray ");
  		}
  		
  		return combinedBytes.toByteArray();

  	}

  	public boolean isitRequiredhash(byte[] hash){
  		return Main.isFirstwbitsZero(hash);
  	}

	public void broadCast(Block blk){
  		Message msg = null;
  		if(DEBUG)
  		System.out.println(nodeId + " Block with block Hash:" + blk.blockHash + " is broadcasted minoerThread");
  		synchronized(Main.messagePassingQ) {
  			System.out.println(nodeId + " MineThread acquired lock on messagePassingQ");
  			for(int i =0; i < Main.numNodes; i++){
  	 
  				msg = new BlockMessage(4, i, nodeId, blk);
  				Queue<Message> recvq = Main.messagePassingQ.get(i);
				recvq.add(msg);
  	  		}
  		}
  		System.out.println(nodeId + " MineThread left lock on messagePassingQ");	
  	}
}
