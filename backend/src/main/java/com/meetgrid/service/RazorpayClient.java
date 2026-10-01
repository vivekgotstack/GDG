package com.meetgrid.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
public class RazorpayClient {
 private final Environment env;
 private final RestClient api;

 public RazorpayClient(Environment env, RestClient.Builder builder) {
  this.env=env;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  factory.setReadTimeout(Duration.ofSeconds(12));
  api=builder.baseUrl("https://api.razorpay.com/v1").requestFactory(factory).build();
 }
 public String keyId(){return env.getProperty("meetgrid.billing.key-id","");}
 public String keySecret(){return env.getProperty("meetgrid.billing.key-secret","");}
 public String webhookSecret(){return env.getProperty("meetgrid.billing.webhook-secret","");}
 public static boolean configured(String value){return value!=null&&!value.isBlank()&&!value.startsWith("REPLACE_");}
 public boolean configured(){return keyId().matches("rzp_(test|live)_[a-zA-Z0-9]+")&&configured(keySecret())&&configured(webhookSecret());}
 public void requireConfigured(){if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Razorpay checkout is not configured yet. Your preview workspace remains available.");}
 public JsonNode get(String path){
  requireConfigured();
  try{return requireBody(api.get().uri(path).headers(h->h.setBasicAuth(keyId(),keySecret())).retrieve().body(JsonNode.class));}
  catch(RestClientException e){throw unavailable();}
 }
 public JsonNode post(String path,Map<String,?> body){
  requireConfigured();
  try{return requireBody(api.post().uri(path).headers(h->h.setBasicAuth(keyId(),keySecret())).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));}
  catch(RestClientException e){throw unavailable();}
 }
 public JsonNode patch(String path,Map<String,?> body){
  requireConfigured();
  try{return requireBody(api.patch().uri(path).headers(h->h.setBasicAuth(keyId(),keySecret())).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));}
  catch(RestClientException e){throw unavailable();}
 }
 private JsonNode requireBody(JsonNode body){if(body==null||!body.isObject())throw unavailable();return body;}
 private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay could not complete this request. Please try again or contact billing support.");}
}
