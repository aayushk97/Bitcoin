import java.math.BigInteger; 
import java.util.*;
import java.security.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Timestamp;


class Block{

	//Block header

	public int minerId;
	public byte[] blockHash; //Hash(nonce||prev||merkelRoot)

	public int height;
	public byte[] prevBlockHash;
	public BigInteger nonce;
	public String timeStampC;
	public String timeStampM;
	public long timeTakentomine;  //in miliseconds.
	public long timeStampInserted;
	
	public Merkle merkleTree; //Merkel tree how?
	
	public Vector<Transaction> transactionsInBlock;
	private Block(){
		//this just to get size of barebone block 
		super();
	}
	public Block(Vector<Transaction> vec, int minerId){		//This constructor is for genesis block only
		this.height = 1;
		this.minerId = minerId;
		byte[] genesis = new byte[32]; //256 because hash size is 256bit
		this.prevBlockHash = genesis;
		this.merkleTree = new Merkle(vec);  //vec is the list of all transaction related to this block		
		//calculate hash of this block using prevHash + nonce + Txns + timeStamp
		addTimeStamp();
		transactionsInBlock = vec;
	}

	public Block(byte[] prevHash, Vector<Transaction> vec, int minerId, int height){
		this.height = height;
		this.minerId = minerId;
		this.prevBlockHash = prevHash;
		this.merkleTree = new Merkle(vec);  //vec is the list of all transaction related to this block		
		//calculate hash of this block using prevHash + nonce + Txns + timeStamp
		addTimeStamp();
		transactionsInBlock = vec;
	}

	public void addTimeStamp(){
		Timestamp tms = new Timestamp(System.currentTimeMillis());
		this.timeStampC = tms.toString();
	}
	
	public void addTimeStampM(){
		Timestamp tms = new Timestamp(System.currentTimeMillis());
		this.timeStampM = tms.toString();
	}
	
	public byte[] getBlockInFormOfbytes(){ //leaving nonce the part that needs to be taken as input to hash
		ByteArrayOutputStream s = new ByteArrayOutputStream();

		try{
			s.write(prevBlockHash);
			s.write(merkleTree.rootHash);
		}catch(IOException e){
			System.out.println("Exception in Block");
		}
		
		try{
			s.write(timeStampC.getBytes("UTF-8"));
		}catch(IOException e){
			System.out.println("Exception");
		}
		return s.toByteArray();
	}

	public long getSize(){
		Block dummy = new Block();
		long x = ObjectSizeFetcher.getObjectSize(dummy);
		long hasheSize = blockHash.length + prevBlockHash.length;
		long y = nonce.toByteArray().length;
		long z = timeStampC.getBytes().length + timeStampM.getBytes().length;
		long a = merkleTree.getSize();
		long size = ObjectSizeFetcher.getObjectSize(transactionsInBlock);
		for(int i = 0; i < transactionsInBlock.size(); i++){
			Transaction txn = transactionsInBlock.get(i);
			size+=txn.getSize();
		}
		return x + hasheSize + y + z + a + size;  	//this give almost complete size of memory 
													//occpied by a Block and its reference objects
	}
}
