import java.security.*;
class UnspentTxn{
	public byte[] txnHash; 
	public int indexInOutOfTxn;
	public PublicKey pk;
	public int receiverId;
	public double amount;  //amount received.
	public UnspentTxn(byte[] txnHash, int indexInOutOfTxn, PublicKey pk, double amount){
		this.txnHash = txnHash;
		this.indexInOutOfTxn = indexInOutOfTxn;
		this.pk = pk;
		this.amount = amount;
		this.receiverId = Main.publicKeymappings.get(this.pk);
	}
	public void print(){
		System.out.println("Ref txn: " + Main.toHexString(txnHash));
		System.out.print("receiverId: " + receiverId + " amount: " + amount);
	}
}
