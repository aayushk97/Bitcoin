class Input{
	public byte[] refTxn;  //Transaction to whose this referes to be spent
	//public byte[] signatureOfSender;
	public int receiptIndex;  //In refTxn the index in which it received the transaction
	
	public Input(byte[] prevTxHash, int index){
		refTxn = prevTxHash;
		receiptIndex = index;
	}
}