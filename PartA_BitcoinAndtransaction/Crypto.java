import java.math.BigInteger; 
import java.security.*;

public class Crypto{
	
	static String hash = Parameters.HASH;

	public static String sha256(String input){ 
		
		try{

			MessageDigest messageDigest = MessageDigest.getInstance(hash);
			byte[] hashInBytes = messageDigest.digest(input.getBytes("UTF-8"));
		
			BigInteger number = new BigInteger(1, hashInBytes);
		
			StringBuilder hashInHex = new StringBuilder(number.toString(16));
		
			while(hashInHex.length() < 32) hashInHex.insert(0, '0');
		
			return hashInHex.toString();
		}catch(Exception e){
			throw new RuntimeException(e);
		}	
	}

	public static byte[] sha256(byte[] input){  
		try{
			MessageDigest messageDigest = MessageDigest.getInstance(hash);
			return messageDigest.digest(input);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		
	}
	
	public static KeyPair generateKeyPair(int keySize){
		try{
			//create a key pair generator object
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		
			//initialize the key pair generator object
			keyPairGen.initialize(keySize, random);
		
			//generate the key pair
			KeyPair pair = keyPairGen.generateKeyPair();
		
			return pair;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	
	}
	
	public static byte[] applyECDSASign(PrivateKey privateKey, byte[] input){
		byte[] signatureResult = new byte[0];
		try{
			
			Signature signature = Signature.getInstance("SHA256withECDSA");
			signature.initSign(privateKey);
			//Add data to the signature
			signature.update(input);
		
			//calculate the signature
			signatureResult = signature.sign();
		
			
		}catch(Exception e){
			System.out.println("Exception: Signature");
		}
		return signatureResult;

	}
	public static byte[] applyECDSASign(PrivateKey privateKey, String input){
		byte[] signatureResult = new byte[0];
		try{
			
			Signature signature = Signature.getInstance("SHA256withECDSA");
			signature.initSign(privateKey);
		
			byte[] bytes = input.getBytes("UTF-8");
		
			//Add data to the signature
			signature.update(bytes);
		
			//calculate the signature
			signatureResult = signature.sign();
		
			
		}catch(Exception e){
			System.out.println("Exception: Signature");
		}
		return signatureResult;

	}
	
	public static boolean verifyECDSASign(PublicKey publicKey, String message, byte[] messageSignature){
		try{
			Signature signature = Signature.getInstance("SHA256withECDSA");
		
			//initialize the signature
			signature.initVerify(publicKey);
			signature.update(message.getBytes());
		
			//verify the signature
			return signature.verify(messageSignature);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public static boolean verifyECDSASign(PublicKey publicKey, byte[] message, byte[] messageSignature){
		try{
			Signature signature = Signature.getInstance("SHA256withECDSA");
		
			//initialize the signature
			signature.initVerify(publicKey);
			signature.update(message);
		
			//verify the signature
			return signature.verify(messageSignature);
		}catch(Exception e){
			System.out.println("Exception: Problem in verifying signature.");
			e.printStackTrace();
		}
	
			return false;
		}
}
