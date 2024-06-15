import java.security.PublicKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class Output{
	public PublicKey receiver;
	public double amount;

	public Output(PublicKey receiver, double amount){
		this.receiver = receiver;
		this.amount = amount;
	}

	private Output(){
		super();
	}

	public byte[] getBytes(){
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try{
			bytes.write(receiver.getEncoded());
			bytes.write(Main.getBytesFromDouble(amount));
		}catch(IOException e){
			System.out.println("Exception");
		}
		
		return bytes.toByteArray();
	}

	public long  getSize(){
		Output dummy = new Output();
		long x = ObjectSizeFetcher.getObjectSize(dummy);
		long pkSize = receiver.getEncoded().length;
		return x + pkSize;
	}
}
