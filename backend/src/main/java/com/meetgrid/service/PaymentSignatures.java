package com.meetgrid.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class PaymentSignatures {
 private PaymentSignatures(){}
 public static boolean verify(String secret,byte[] message,String signature){
  if(secret==null||secret.isBlank()||signature==null||!signature.matches("[a-fA-F0-9]{64}"))return false;
  try{
   var mac=Mac.getInstance("HmacSHA256");
   mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
   return MessageDigest.isEqual(mac.doFinal(message),HexFormat.of().parseHex(signature));
  }catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Payment signature verification is unavailable",e);}
 }
 public static boolean verify(String secret,String message,String signature){return verify(secret,message.getBytes(StandardCharsets.UTF_8),signature);}
}
