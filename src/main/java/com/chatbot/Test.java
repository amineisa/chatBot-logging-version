package com.chatbot;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.BadPaddingException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import sun.misc.BASE64Decoder;

public class Test {

	public static void main(String[] args) {
		
		String m = "6e4b7441746f53706a4658784965544653614f58735444546b46644161663167727a3253343556576a6e316f447a6c594d42712b723259566f716c554a52664550654b683537356d364d4e730d0a63745a615065526e45673d3d";
		try {
			System.out.println(decryptChannelParam(m));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	 public static String decryptChannelParam(String encryptedParams) throws Exception {
	        String simiDecreptedParams = new String(hexToString(encryptedParams), "UTF8");
	        byte keyBytes[] = "etisalatetisalat".getBytes();
	        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
	        Cipher dcipher = Cipher.getInstance("AES");
	        dcipher.init(Cipher.DECRYPT_MODE, secretKey);
	        byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(simiDecreptedParams);
	        byte[] utf8 = null;
	        utf8 = dcipher.doFinal(dec);

	        return new String(utf8, "UTF8");
	    }
	
	 
	 
	 
	 private static byte[] hexToString(String str) {
	        byte[] bytes = new byte[str.length() / 2];
	        for (int i = 0; i < bytes.length; i++) {
	            bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
	        }
	        return bytes;
	    }
}
