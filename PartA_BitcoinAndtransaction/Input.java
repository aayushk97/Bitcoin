class Input{
	public byte[] refTxn;  //Transaction hash to whose this input referes to be spent
	public int receiptIndex;  //In refTxn output the index in which it received the amount
	
	public Input(byte[] prevTxHash, int index){
		refTxn = prevTxHash;
		receiptIndex = index;
	}
}