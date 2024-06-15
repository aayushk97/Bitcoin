import java.math.BigInteger;
import java.util.*;
import java.security.*;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;

public class Node implements Runnable{
	public final boolean DEBUG = false;
	static final BigInteger ONE = new BigInteger("1");
	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	public Vector<Block> bitcoinChain;
	public Thread tId;
	public MineThread mineThreadNode;
	public TransactionThread txnThreadNode ;
	
	public int nodeId;
	
	
	public HashMap<PublicKey, Vector<UnspentTxn>> unspentTxns;

	public Vector<Transaction> validTransactions;
	
	public Set<byte[]> transactionsInBlockChain;
	
	private boolean reqWasSent = false;
	
	private boolean isHonest;
	private int startOfDishonests;

	private boolean printed = false;
	
	AtomicBoolean minedFirst = new AtomicBoolean(true);
	
	public Node(int nodeId){
		//this.bitcoinChain = bitcoinChain;  //ideally it should probe all other nodes to get longest chain.
		Main.logger.info("Node: "+ nodeId +  " Created");
		//Generate keys for this node
		this.bitcoinChain = new Vector<>();
		this.nodeId = nodeId;
		this.isHonest = true;
		this.startOfDishonests = Main.numNodes;

		int size = 256; 
		KeyPair pair = Crypto.generateKeyPair(size);
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();

		transactionsInBlockChain = new HashSet<byte[]>();
		
		//initialize unspect queues
		unspentTxns = Main.unspentPools.get(this.nodeId);
		
		validTransactions = new Vector<>();
		
	}
	
	//constructor for dishonest nodes
	public Node(int nodeId, int startNodeIdOfDishonests){
		//this.bitcoinChain = bitcoinChain;  //ideally it should probe all other nodes to get longest chain.
		Main.logger.info("Node: "+ nodeId +  " Created");
		//Generate keys for this node
		this.bitcoinChain = new Vector<>();
		this.nodeId = nodeId;
		this.isHonest = false;
		this.startOfDishonests = startNodeIdOfDishonests;  //from this iindex onwards are dihones nodes.

		int size = 256; 
		KeyPair pair = Crypto.generateKeyPair(size);
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();

		transactionsInBlockChain = new HashSet<byte[]>();
		
		//initialize unspect queues
		unspentTxns = Main.unspentPools.get(this.nodeId);
		
		validTransactions = new Vector<>();
	}
	

	public void run(){
		//System.out.println("Message Checking System " + nodeId);
		
		if(isHonest){
			runAnHonestNode();
		}else{
			runDishonestNode();
		}
		Main.logger.info("Node: "+ nodeId +  " Exiting running...");
	}
	
	public void runDishonestNode(){
		//System.out.println(nodeId + " Running DisHonest");
		Main.logger.fine("Node: "+ nodeId +  " Started running a Honest node");
		
		while(true){
			//System.out.println(nodeId + " restarting loop:");
			if(Main.nodes.get(nodeId).bitcoinChain.size() >= Main.stop && Main.messagePassingQ.get(nodeId).isEmpty()) break;
			
			if(!messageQEmpty()){
				if(DEBUG)
				System.out.println("No of message in buffer for node: " + 
					nodeId +" is : " + Main.messagePassingQ.get(nodeId).size());

				Message msg = getMessage();
				
				switch(msg.messageType){
					case 1:
						if(DEBUG)
							System.out.println("Handling request message " + nodeId);
						RequestMessage reqMsg = (RequestMessage)msg;
						sendResponseMessage(reqMsg.sender);
						break;
					case 2:
						if(DEBUG)
							System.out.println("Handling response message " + nodeId);

						ResponseMessage resMsg = (ResponseMessage)msg;	
						handleResponseMessage(resMsg);
						break;
					case 3:
						if(DEBUG)
							System.out.println("Handling transaction message " + nodeId);

						TransactionMessage txnMsg = (TransactionMessage)msg;
						handleTransactionMessage(txnMsg);
						break;
					case 4:
						if(DEBUG)
							System.out.println("Handling Block message " + nodeId);
						
						BlockMessage blkMsg = (BlockMessage)msg;
						//System.out.println("Handling Block message " + blkMsg.blk.blockHash);
						handleBlockMessage(blkMsg);
						break;
					default:
						if(DEBUG)
							System.out.println("No block matched" + nodeId);		
				}				
			}
			///System.out.println(nodeId + " finishing loop");		
		}
	}

	public void runAnHonestNode(){

		System.out.println(nodeId + " Running Honest");
		Main.logger.fine("Node: "+ nodeId +  " Started running a Honest node");

		while(true){
			//if(!printed)
			//	printState();
			//System.out.println(nodeId + " restarting loop");
			if(Main.nodes.get(nodeId).bitcoinChain.size() > Main.stop && Main.messagePassingQ.get(nodeId).isEmpty()) break;
			
			if(!messageQEmpty()){
				if(DEBUG)
				System.out.println("No of message in buffer for node: " + 
					nodeId +" is : " + Main.messagePassingQ.get(nodeId).size());

				Message msg = getMessage();
				
				switch(msg.messageType){
					case 1:
						if(DEBUG)
						System.out.println("Handling request message entered " + nodeId);
						RequestMessage reqMsg = (RequestMessage)msg;
						sendResponseMessage(reqMsg.sender);
						//System.out.println("Handling request message returned " + nodeId);
						break;
					case 2:
						if(DEBUG)
						System.out.println("Handling response message entered " + nodeId);

						ResponseMessage resMsg = (ResponseMessage)msg;	
						handleResponseMessage(resMsg);
						//System.out.println("Handling response message returned " + nodeId);
						break;
					case 3:
						if(DEBUG)
						System.out.println("Handling transaction message entered " + nodeId);

						TransactionMessage txnMsg = (TransactionMessage)msg;
						handleTransactionMessage(txnMsg);
						//System.out.println("Handling transaction message returned " + nodeId);
						break;
					case 4:
						if(DEBUG)
						System.out.println("Handling Block message entered " + nodeId);
						
						BlockMessage blkMsg = (BlockMessage)msg;
						//System.out.println("Handling Block message " + blkMsg.blk.blockHash);
						handleBlockMessage(blkMsg);
						//System.out.println("Handling Block message returned " + nodeId);
						break;
					default:
						if(DEBUG)
							System.out.println("No block matched" + nodeId);		
				}				
			}
					
		}
		System.out.println(nodeId + " finishing myWork" );	
	}

	public void start(){
		//will start the thread and call initialization method.
		System.out.println("Starting Thread: " + nodeId);
		if(tId == null){
			tId = new Thread(this);
			tId.start();
		}
		
		mineThreadNode = new MineThread(nodeId, publicKey, privateKey, isHonest, startOfDishonests);
		txnThreadNode = new TransactionThread(nodeId, publicKey, privateKey, isHonest, startOfDishonests);
		
		mineThreadNode.start();
		txnThreadNode.start();
	}
	
	public void initializeBitcoinChain(){
		synchronized(Main.unspentPools.get(this.nodeId)){
			for(int i =0; i < Main.numNodes; i++){
				unspentTxns.put(Main.nodes.get(i).publicKey, new Vector<UnspentTxn>());
			}
		}
		synchronized(Main.publicKeymappings){
			Main.publicKeymappings.put(publicKey, nodeId);
		}
		
	}
	
	public void printState(){
		synchronized (Main.printingMutex){
			synchronized(bitcoinChain){
				int size = bitcoinChain.size();
				if(size == 1){
					System.out.println("\nAfter genesis Node: " + nodeId + " state.");
					System.out.println("Blockchain size: "+ size );
					System.out.println("Hash of Current block: " + Main.toHexString(bitcoinChain.get(size - 1).blockHash) );
					System.out.println("Block Miner: " + bitcoinChain.get(size - 1).minerId);
					printUnpentpool();
					System.out.println();
					printed = true;
				}
			}
		}
	}

	public void printUnpentpool(){
		synchronized(Main.unspentPools.get(this.nodeId)){
			//HashMap<PublicKey, Vector<UnspentTxn>> unspentTxns;
			System.out.println("Printing Unspent outputs pool of nodeId " + nodeId);
			for(Map.Entry<PublicKey, Vector<UnspentTxn>> entry : unspentTxns.entrySet()){
				int id = Main.publicKeymappings.get(entry.getKey());
				Vector<UnspentTxn> vec = entry.getValue();
				//System.out.println("Utxo of")
				for(int i =0; i < vec.size(); i++){
					UnspentTxn utxo = vec.get(i);
					utxo.print();
				}
			}
		}
	}

	public Transaction createCoinbaseTxn(){
		Transaction coinbaseTxn = new Transaction(true, this.publicKey, nodeId);
		return coinbaseTxn;
	}
	
	public void mineGenesisBlock(){
		Transaction txn = createCoinbaseTxn();	//Only a coinbase txn will be added in the genesis block
		signTransaction(txn);
		
		//Store all the created transaction
		Vector<Transaction> vec = new Vector<>();
		vec.add(txn);

		byte[] prevHash = new byte[32];
		if(bitcoinChain.size() > 0){
			prevHash = bitcoinChain.get(bitcoinChain.size() -1).blockHash;
		}
		
		//Add the transactions to block
		Block blk = new Block(vec, nodeId);
		//System.out.println("Block was mined with hash : "+blk.blockHash);
		POW(blk);
		
		broadCast(blk);
		
		//System.out.println("Block was mined with hash : "+blk.blockHash);
		//bitcoinChain.add(blk);
		if(DEBUG)
		System.out.println("Genesis block mined by " + nodeId + " at " + getCurrentTime());
		
		//add this transaction to unspent trnasaction
		//UnspentTxn myUnspentTxn = new UnspentTxn(txn.txHash, 0, publicKey, txn.outputTxns.get(0).amount);
		//System.out.println("Amount in genesis mine block: "+txn.outputTxns.get(0).amount);
		/*synchronized(Main.unspentPools.get(this.nodeId)){
			unspentTxns.get(publicKey).add(myUnspentTxn);
		}*/	
	}

	public boolean validateTransaction(Transaction txn){
		synchronized(Main.unspentPools.get(this.nodeId))
		{
			Vector<UnspentTxn> unspentTxnNode = unspentTxns.get(txn.sender);
		
		/*double amountPresent = 0.0, amountTransferred = 0.0;
		//synchronized(unspentTxns.get(publicKey)){
		//Get total unspent Transaction
		for(UnspentTxn t: unspentTxnNode){
			amountPresent += t.amount;
			
			//System.out.println("The transaction node is:"+t.txnHash+" amount is :"+t.amount);
		} 
		
		//Get the amount that was sent in transaction
		for(int i = 0; i < txn.outputTxns.size(); i++){
			amountTransferred += txn.outputTxns.get(i).amount;
			// System.out.println("The transaction block hash is:"+txn.outputTxns.get(i).receiver
			// 	+" and amount is :"+txn.outputTxns.get(i).amount);
			
		}
		amountPresent = (double)Math.round(amountPresent * 1000000d)/1000000d;
		amountTransferred = (double)Math.round(amountTransferred * 1000000d)/1000000d;
		if(DEBUG)
		System.out.println("Amount Present :"+amountPresent+"Amount Transferred: "+amountTransferred
			+ " received in node ID: "+nodeId);
		//The transaction is valid
		if(amountPresent == amountTransferred){
		*/	
			for(int i = 0; i < txn.inputTxns.size(); i++){
				boolean found = false;
				for(int j = 0; j < unspentTxnNode.size(); j++){
					if(Arrays.equals(unspentTxnNode.get(j).txnHash, txn.inputTxns.get(i).refTxn)) {
						found = true;
						break;
						// System.out.println(nodeId + " found input utxo: with txn hash " + 
						// 	Main.toHexString(txn.inputTxns.get(i).refTxn).substring(0, 15));
					}
					//System.out.println("The ref txn was : "+ txn.inputTxns.get(i).refTxn+" and transactions present is: "+unspentTxnNode.get(j).txnHash); ;
				}
				
				if(found == false){
					return false;
				}
				
			}
			
			return true;
		}
		
		
		
	}
	
	
	public void updateUTXOpool(Transaction txn){
		synchronized(Main.unspentPools.get(nodeId)){

			Vector<UnspentTxn> unspentTxnNode = unspentTxns.get(txn.sender);

			for(int j = 0; j < txn.inputTxns.size(); j++){
					
				int i = 0;
				while(i < unspentTxnNode.size()){
					if(Arrays.equals(unspentTxnNode.get(i).txnHash , txn.inputTxns.get(j).refTxn)){
						unspentTxnNode.remove(i);
					}else{
						i++;
					}
				}
			}
			
			//Add new unspent transactions
			for(int i = 0; i < txn.outputTxns.size(); i++){
				UnspentTxn unspent = new UnspentTxn(txn.txHash, i, txn.outputTxns.get(i).receiver, 
					txn.outputTxns.get(i).amount);

				unspentTxns.get(txn.outputTxns.get(i).receiver).add(unspent);
			}
		}
		
	}
	
	public void updateUTXOpoolSelf(Transaction txn){
		synchronized(Main.unspentPools.get(nodeId)){
			for(int i = 0; i < txn.outputTxns.size(); i++){
				UnspentTxn unspent = new UnspentTxn(txn.txHash, i, txn.outputTxns.get(i).receiver, txn.outputTxns.get(i).amount);
				unspentTxns.get(txn.outputTxns.get(i).receiver).add(unspent);
			}
		}
	}
	

  	public boolean POW(Block blk){ //while mining it needs to keep checking whether next block has arrived
		
  		byte[] s = blk.getBlockInFormOfbytes(); //Should return bytes of prevHash+Txns(merkel root)
  		BigInteger nonce = new BigInteger("0");
  		//System.out.println("Size of the block" + Main.blockReceivingQueues.get(nodeId).size());
  		while(true){
  			//byte[] toBeHashed = concatTwoByteArray(nonce.toByteArray(), s);

  			 byte[] nonceBytes = getNonceByte(nonce);   //need to use this function because we have to plot noce size vs time.
  			 byte[] toBeHashed = concatTwoByteArray(nonceBytes, s);

  			try{

  				byte[] hash = Crypto.sha256(toBeHashed);
	            if(isitRequiredhash(hash)){
	            	blk.nonce = nonce;
	            	blk.blockHash = hash;
	            	//System.out.println(nodeId + ": Hash: " + Main.toHexString(hash) + " block height: " + blk.height);
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
  			//System.out.println("Need to increase nonce size");
  			Main.nonceSize += array.length + 5;
  			out = new byte[Main.nonceSize];
  		}
  		int diff = out.length - array.length;
  		for(int i = 0; i < array.length; i++){
  			out[i + diff] = array[i];
  		}
  		return out;
  	}

  	public boolean verifyBlock(Block blockToVerify){
  		if(verifyBlockHash(blockToVerify)){
  			return true;  //Still transactions need to be verified
  		}else{
  			return false;
  		}
  	
  	}

  	
  	public void signTransaction(Transaction txn){

        byte[] rawData = Main.nodes.get(nodeId).getTxnBytes(txn);
        byte[] signature = Crypto.applyECDSASign(this.privateKey, rawData);
        txn.txHash = Crypto.sha256(rawData);
        txn.signature = signature;

    }
 
  	public boolean isitRequiredhash(byte[] hash){
  		return Main.isFirstwbitsZero(hash);
  	}

  	public void sendRequestMessage(int receiver){  
		
		if(DEBUG)
		System.out.println("RequestMessage was sent by: "+nodeId+" to node"+receiver);
  		Message msg = new RequestMessage(1, receiver, this.nodeId, this.bitcoinChain.size());

  		synchronized(Main.messagePassingQ){
  			Main.messagePassingQ.get(receiver).add(msg);
  			//Main.messagePassingQ.get(receiver).notify();
  		}
  	}

  	public String getCurrentTime(){
  		Timestamp tms = new Timestamp(System.currentTimeMillis());
		return tms.toString();
  	}
  	public void broadCast(Block blk){
  		Message msg = null;
  		if(DEBUG)
  		System.out.println(nodeId + " Block with block Hash:" + blk.blockHash + " is broadcasted minoerThread");
  		synchronized(Main.messagePassingQ) {
  			for(int i =0; i < Main.numNodes; i++){
  	  			//if(i != nodeId){
  	  				msg = new BlockMessage(4, i, nodeId, blk);
  	  				
  	  				Queue<Message> recvq = Main.messagePassingQ.get(i);
  		  			
	  				recvq.add(msg);
	  				//recvq.notify();
  		  			
  	  			//}
  	  			
  	  		}
  		}
  		
  	}
  	
  	public PublicKey getPublickey(){
		return publicKey;
	}
	
  	public byte[] getTxnBytes(Transaction txn){
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    	PublicKey sender = txn.sender;
    	try{
        		bytes.write(sender.getEncoded());
    	}catch(IOException e){
        		System.out.println(nodeId + " Exception");
    	}
        	
		int inpSize = txn.inputTxns.size();
		for(int i = 0; i < inpSize; i++){
			try{
				bytes.write(txn.inputTxns.get(i).refTxn);

			}catch(IOException e){
				System.out.println("Exception");
			}
		}

		int outSize = txn.outputTxns.size();
		for (int i = 0; i < outSize; i++ ){
			try{
				bytes.write(txn.outputTxns.get(i).getBytes());
				
			}catch(IOException e){
				System.out.println("Exception");
			}
		}

		try{
			bytes.write(txn.timeStamp.getBytes("UTF-8"));
		}catch(IOException e){
			System.out.println("Exception");
		}
		
		return bytes.toByteArray();
	}
	
  	public boolean verifyTxnSignature(Transaction txn){
  		byte[] rawData = getTxnBytes(txn);
  		return Crypto.verifyECDSASign(txn.sender, rawData, txn.signature);
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
  	
  	public boolean verifyBlockHash(Block blk){

  		byte[] blockHash = blk.blockHash;
  		byte[] nonceBytes = getNonceByte(blk.nonce);   //need to use this function because we have to plot noce size vs time.
  		byte[] x = concatTwoByteArray(nonceBytes, blk.getBlockInFormOfbytes());
  		//byte[] x = concatTwoByteArray(blk.nonce.toByteArray(), blk.getBlockInFormOfbytes());
  		byte[] calculatedHash = Crypto.sha256(x);
  		
  		boolean verifyStoredHash = Arrays.equals(blockHash, calculatedHash);
  		
  		
  		if(!verifyStoredHash) return false;
  		
  		boolean verifyPrevHash = true;
  		if(bitcoinChain.size() > 1) verifyPrevHash = Arrays.equals(blk.prevBlockHash,
  		 bitcoinChain.get(bitcoinChain.size() -1).blockHash);
  		
  		if(!verifyPrevHash) return false;
  		
  		return true;
  	}
  	
  	public boolean verifyTransaction(Transaction txn){
	 
		boolean verified = verifyTxnSignature(txn);	
		if(!verified) 
			return false;
		
		boolean validate = validateTransaction(txn);
		
		if(!validate) 
			return false;
		
		if(transactionsInBlockChain.contains(txn.txHash)) 
			return false;
		return true;
	}
	
  	
  	public void handleResponseMessage(ResponseMessage msg){
  		if(Arrays.equals(msg.getBlockChain().get(msg.getBlockChain().size()-1).blockHash, 
  			bitcoinChain.get(bitcoinChain.size()-1).blockHash)) return;
  		
  		if(msg.getBlockChain().size() < bitcoinChain.size()) return;
  		if(msg.getBlockChain().size() == bitcoinChain.size()){
  		String time = msg.getBlockChain().get(msg.getBlockChain().size()-1).timeStampM;
		// Split path into segments
		String segments[] = time.split(":");
		// Grab the last segment
		double seconds1 = Double.parseDouble(segments[segments.length - 1]);
		double minutes1 = Double.parseDouble(segments[segments.length -2]);
		
		String time2 = bitcoinChain.get(bitcoinChain.size()-1).timeStampM;
		// Split path into segments
		String segments2[] = time2.split(":");
		// Grab the last segment
		double seconds2 = Double.parseDouble(segments2[segments2.length - 1]);
		double minutes2 = Double.parseDouble(segments2[segments2.length - 2]);
		
		//System.out.println(" The time of received chain: "+minutes1+":"+seconds1+" and our time is: "+minutes2+":"+seconds2);
		if(minutes2 < minutes1) return;
		if(minutes2 == minutes1 && seconds2 < seconds1) return ;
  		}
  		
			//check to see if chain is valid
			for(int i = 1; i < msg.getBlockChain().size(); i++){
				if((msg.getBlockChain().get(i).prevBlockHash != msg.getBlockChain().get(i-1).blockHash)){
				if(DEBUG)
				System.out.println("The block chain is not valid as parent Hash dont match!");
				//Main.messagePassingQ.get(msg.sender).notify();
				reqWasSent = false;
				return ;
				}
				
				byte[] blockHash = msg.getBlockChain().get(i).blockHash;
				byte[] nonceBytes = getNonceByte(msg.getBlockChain().get(i).nonce);   
				//need to use this function because we have to plot noce size vs time.
  				byte[] x = concatTwoByteArray(nonceBytes, msg.getBlockChain().get(i).getBlockInFormOfbytes());
				//byte[] x = concatTwoByteArray(msg.getBlockChain().get(i).nonce.toByteArray(), 
				//msg.getBlockChain().get(i).getBlockInFormOfbytes());
				byte[] calculatedHash = Crypto.sha256(x);
	
				boolean verifyStoredHash = Arrays.equals(blockHash, calculatedHash);
				if(!verifyStoredHash){
					if(DEBUG)
					System.out.println("The block chain is not valid as block chain hash dont match!:");		
					reqWasSent = false;
					return ;			  					
				}
			}
			if(bitcoinChain.size() <= msg.getBlockChain().size()){
				bitcoinChain = msg.getBlockChain();
				unspentTxns = msg.unspentTxns;
				transactionsInBlockChain = msg.transactionsInChain;
			}
			
			reqWasSent = false;
  	}
  	
  	public void handleTransactionMessage(TransactionMessage msg){
  		if(msg.sender == nodeId) {
  			validTransactions.add(msg.txn);
  			return;
  		}
  		Transaction receivedTxn = msg.txn;
  		
  		synchronized(validTransactions){
			if(transactionsInBlockChain.contains(msg.txn.txHash)) return;
			boolean verified = verifyTransaction(receivedTxn);
			if(verified){
				//synchronized(validTransactions){
				validTransactions.add(receivedTxn);
				//System.out.println(nodeId + " ValidTxnSize from node.java + size after adding " + validTransactions.size());
				//}	
				//if(DEBUG)
				// System.out.println(nodeId + "Transaction was added: txn hash " 
				// 	+ Main.toHexString(receivedTxn.txHash).substring(0, 15));	
			}else{
				if(!isHonest && receivedTxn.senderId >= startOfDishonests){
		  			validTransactions.add(receivedTxn);
		  			//System.out.println(nodeId + " Dis Transaction was added: txn hash " + Main.toHexString(receivedTxn.txHash).substring(0, 15));
		  				try{
		  					Thread.sleep(10);	
		  				}catch (InterruptedException e){
		  					System.out.println("get up");
		  				}
						
		  			return;
	  			}
				//System.out.println(nodeId + "Transaction invalid:no added txn hash " + receivedTxn.txHash);
			}
		}
  	}
  	
  	public void handleBlockMessage(BlockMessage msg){
  		
  		Block blk = msg.blk;
  		if(DEBUG)
  		System.out.println( nodeId + "Height of block received: "+ blk.height 
  			+ " miner: " + blk.minerId 
  			+ " blockChain height" + bitcoinChain.size());

  		if(blk.height == bitcoinChain.size() + 1){ 
	  		
	  		if(!verifyBlockHash(blk)){
	  			//System.out.println("This Block is not verified!");
	  		 	return; 		 	
	  		 }
	  		
	  		if(isHonest && msg.sender < startOfDishonests){	  		
	  		for(int i = 0; i < blk.transactionsInBlock.size(); i++){
	  			
	  			Transaction txn = blk.transactionsInBlock.get(i);
	  			if(!txn.coinbase && transactionsInBlockChain.contains(txn.txHash)){ 
	  				if(DEBUG)
	  				System.out.println(nodeId + " there was already this transaction present minde by: " + blk.minerId);
	  				consensus(msg.sender);
	  				return;
	  			}
	  			if(!txn.coinbase && txn.sender != this.publicKey && !validateTransaction(txn)){
	  				if(DEBUG)
	  				System.out.println("Transaction was not valid");
	  				return;
	  			}
	  		}
	  		}
	  		
	  		
	  		for(int i = 0; i < blk.transactionsInBlock.size(); i++){
	  			Transaction txn = blk.transactionsInBlock.get(i);
	  			transactionsInBlockChain.add(txn.txHash);
	  			if(DEBUG)
	  			System.out.println("Transaction in block was coinbase?: "+txn.coinbase);
	  			if(txn.coinbase){
	  				UnspentTxn unspentOthersTxn = new UnspentTxn(txn.txHash, 0,  txn.outputTxns.get(0).receiver, 
	  					txn.outputTxns.get(0).amount); 
			    		unspentTxns.get(txn.outputTxns.get(0).receiver).add(unspentOthersTxn);
	  			 	if(DEBUG)
	  			 	System.out.println(msg.sender+"'s Coinbase transaction was added by "+nodeId
	  			 		+"in list of unspent transactions");
	  			
	  			}else{
	  				if(txn.sender == this.publicKey) 
	  					updateUTXOpoolSelf(txn);
	  				else 
	  					updateUTXOpool(txn);
	  			} 
	  		}
	  		
	  		bitcoinChain.add(blk);
	  		minedFirst.set(false);
	  		if(DEBUG)
	  		System.out.println("Block was added: " + nodeId + " with block hash "+blk.blockHash);
		}else if(blk.height < bitcoinChain.size()){
			if(msg.sender!= nodeId)
				sendResponseMessage(msg.sender);
			if(DEBUG)
			System.out.println("The block sending Id is wrong");
		}else if(blk.height == bitcoinChain.size()){
			if(DEBUG)
			System.out.println("The block was not added as block of same height exists");
			return ;
		}
		else{
			if(DEBUG)
			System.out.println(nodeId + " Something is wrong. Initiating consensus! received blk miner: " + blk.minerId);
			if(msg.sender != nodeId)
				consensus(msg.sender);
		}
  	}
  	
  	
  	public void consensus(int receiverNodeId){
  		if(DEBUG)
		System.out.println("Consensus Algorithm initiated by node: "+nodeId+"sending to node "+receiverNodeId);
		//Vector<Block> bChain = (Vector)bitcoinChain.clone();
		if(DEBUG)
		System.out.println("before from wait");
		
		sendRequestMessage(receiverNodeId);
		reqWasSent = true;
	}

	public boolean messageQEmpty(){		
		synchronized(Main.messagePassingQ.get(nodeId)){
			return Main.messagePassingQ.get(nodeId).isEmpty();
		}
	}
  	public Message getMessage(){
  		Message msg =null;
  		synchronized(Main.messagePassingQ){
  			if(!Main.messagePassingQ.get(nodeId).isEmpty())
  				msg  =  Main.messagePassingQ.get(nodeId).remove();
  				//Main.messagePassingQ.get(nodeId).notify();
  		}
  		return msg;
  	}
  	
  	public void sendResponseMessage(int receiver){  
		if(DEBUG)
		System.out.println("Sending Response Message to :" + receiver);
		Vector<Block> bitcoinChainCopy = cloneMychain(bitcoinChain);
		HashMap<PublicKey, Vector<UnspentTxn>> utxoCopy = copyOfMyUnspentPool();
		
		Set<byte[]> txnInChain = copyTxnInBlocks(transactionsInBlockChain);
  		Message msg = new ResponseMessage(2, receiver, this.nodeId, bitcoinChainCopy, utxoCopy, txnInChain);

  		synchronized(Main.messagePassingQ){
  			Main.messagePassingQ.get(receiver).add(msg);
  			//Main.messagePassingQ.get(receiver).notify();
  		}
  	}

  	public  Set<byte[]> copyTxnInBlocks(Set<byte[]> txnAll){
  		Set<byte[]> set = new HashSet<>();
  		for(byte[] arr : txnAll){
  			byte[] x = arr.clone();
  			set.add(x);
  		}
  		return set;
  	}
  	public Vector<Block> cloneMychain(Vector<Block> chain){
  		synchronized(chain){
  			Vector<Block> vec = new Vector<>();
  			for(int i = 0; i < chain.size(); i++ ){
  				vec.add(chain.get(i));
  			}
  			return vec;
  		}
  	}
  	public void copyUnspentPool(HashMap<PublicKey, Vector<UnspentTxn>> utxPool){

  		synchronized(Main.unspentPools.get(nodeId)){
  			System.out.println("Here...\n\n");
  			Set<PublicKey> keys = unspentTxns.keySet();
  			for(PublicKey key : keys){
  				Vector<UnspentTxn> vec =  unspentTxns.get(key);
  				Vector<UnspentTxn> vec2 = utxPool.get(key);
  				vec.clear();
  				for(int i = 0; i < vec2.size(); i++){
  					vec.add(vec2.get(i));
  				}
  			}
  		}
  	}

  	public HashMap<PublicKey, Vector<UnspentTxn>> copyOfMyUnspentPool(){
  		HashMap<PublicKey, Vector<UnspentTxn>> utxPool = new HashMap<>();

  		synchronized(Main.unspentPools.get(nodeId)){
  			Set<PublicKey> keys = unspentTxns.keySet();
  			for(PublicKey key : keys){
  				Vector<UnspentTxn> vec =  unspentTxns.get(key);
  				Vector<UnspentTxn> vec2 = new Vector<>();
  				
  				for(int i = 0; i < vec.size(); i++){
  					vec2.add(vec.get(i));
  				}
  				utxPool.put(key, vec2);
  			}
  			return utxPool;
  		}
  	}

  	public void print(byte[] arr){
  		System.out.println(Main.toHexString(arr).substring(0,10));
  	}
}
