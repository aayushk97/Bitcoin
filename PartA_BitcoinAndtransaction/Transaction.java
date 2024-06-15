import java.security.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;

class Transaction{
	public PublicKey sender;
	public int senderId;
	public byte[] txHash;
	public byte[] signature;
	public String timeStamp;
	public Vector<Input> inputTxns;
	public Vector<Output> outputTxns;
	public long transactionCreated;

	public boolean coinbase;  //for mining reward 
	public Transaction(PublicKey sendersPublickey, int senderId){
		this.senderId = senderId;
		this.coinbase = false;
		this.sender = sendersPublickey;
		inputTxns = new Vector<>();
		outputTxns = new Vector<>();
		transactionCreated = System.nanoTime();
		addTimeStamp();
	}
	public Transaction(boolean coinbase, PublicKey minersPublickey, int senderId){
		this.senderId = senderId;
		this.coinbase = true;
		inputTxns = new Vector<>();
		outputTxns = new Vector<>();
		this.sender = minersPublickey;
		addTimeStamp();
		transactionCreated = System.nanoTime();
		Output out = new Output(minersPublickey, Main.MINING_REWARD);
		outputTxns.add(out);

	}
	
	private Transaction(){
		//only to get size of TXn objects
		super();
	}
	public void addTimeStamp(){
		Timestamp tms = new Timestamp(System.currentTimeMillis());
		this.timeStamp = tms.toString();
	}
	
	public void addOutputToTxn(PublicKey receiver, double amt){
		Output out = new Output(receiver, amt);
		outputTxns.add(out);
	}

	public void addInputToTxn(byte[] refTxnHash, int indexInTheTxnsOutput){
		Input inp = new Input(refTxnHash, indexInTheTxnsOutput);
		inputTxns.add(inp);
	}
	public long getSize(){
		Transaction dummy = new Transaction();
		long x = ObjectSizeFetcher.getObjectSize(dummy);
		long pkSize = sender.getEncoded().length;
		long hashplusSignSize = txHash.length + signature.length;
		long z = timeStamp.getBytes().length;
		long sizeInp = ObjectSizeFetcher.getObjectSize(inputTxns);
		for(int i = 0; i < inputTxns.size(); i++){
			Input inp = inputTxns.get(i);
			sizeInp+= ObjectSizeFetcher.getObjectSize(inp);
		}
		long sizeOp = ObjectSizeFetcher.getObjectSize(outputTxns);
		for(int i = 0; i < outputTxns.size(); i++){
			Output op = outputTxns.get(i);
			sizeOp+= op.getSize();
		}
		return x + pkSize + hashplusSignSize + z + sizeInp + sizeOp;
	}

	public byte[] getHash(){ return txHash;}
}
