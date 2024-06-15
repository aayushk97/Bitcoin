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
}