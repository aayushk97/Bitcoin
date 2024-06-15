import java.util.*;
import java.security.*;

class Message{
	public int messageType; // 1 for request; 2 for send; 3 for transaction; 4 for block
	public int receiver;
	public int sender;

	public Message(int messageType, int receiver, int sender){
		this.messageType = messageType;
		this.receiver = receiver;
		this.sender = sender;
	}
}

class RequestMessage extends Message{
	public int chainSize;
	public RequestMessage(int messageType, int receiver, int sender, int chainSize){
		super(messageType, receiver, sender);
		this.chainSize = chainSize;
	}
}

class ResponseMessage extends Message{
	private Vector<Block> bitcoinChain;
	public HashMap<PublicKey, Vector<UnspentTxn>> unspentTxns;
	public Set<byte[]> transactionsInChain;
	public ResponseMessage(int messageType, int receiver, int sender,  Vector<Block> bitcoinChain, HashMap<PublicKey, Vector<UnspentTxn>> utxo, Set<byte[]> tIC){
		super(messageType, receiver, sender);
		this.bitcoinChain = bitcoinChain;
		this.unspentTxns = utxo;
		this.transactionsInChain = tIC;
	}
	
	public Vector<Block> getBlockChain(){
		return bitcoinChain;
	}

}

class TransactionMessage extends Message{
	public Transaction txn;
	
	public TransactionMessage(int messageType, int receiver, int sender, Transaction txn){
		super(messageType, receiver, sender);
		this.txn = txn;	
	}
}

class BlockMessage extends Message{
	public Block blk;	
	public BlockMessage(int messageType, int receiver, int sender, Block blk){
		super(messageType, receiver, sender);
		this.blk = blk;
	}
}


