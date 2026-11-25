package com.edu.unimag.rodatours.media;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CloudinaryImageProvider implements ImageProvider {
    private final CloudinaryProperties properties;private final RestClient client;
    @Autowired public CloudinaryImageProvider(CloudinaryProperties properties){this(properties,client());}
    CloudinaryImageProvider(CloudinaryProperties properties,RestClient client){this.properties=properties;this.client=client;}
    private static RestClient client(){var f=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());f.setReadTimeout(Duration.ofSeconds(15));return RestClient.builder().baseUrl("https://api.cloudinary.com/v1_1").requestFactory(f).build();}
    private void configured(){CloudinaryFailures.configured(properties,properties.getImageUploadPreset());}
    private String sign(Map<String,String> values){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((new TreeMap<>(values).entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(Collectors.joining("&"))+properties.getApiSecret()).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("No se pudo firmar la carga.");}}
    @Override public VideoProvider.UploadAuthorization authorize(String id){configured();Map<String,String> p=Map.of("public_id",id,"timestamp",Long.toString(Instant.now().getEpochSecond()),"upload_preset",properties.getImageUploadPreset(),"overwrite","false");return new VideoProvider.UploadAuthorization("https://api.cloudinary.com/v1_1/"+properties.getCloudName()+"/image/upload",properties.getApiKey(),p,sign(p));}
    @Override public ImageMetadata inspect(String id){configured();try{
        Map<String,Object> d=client.get().uri("/{cloud}/resources/image/upload/{id}",properties.getCloudName(),id).headers(h->h.setBasicAuth(properties.getApiKey(),properties.getApiSecret())).retrieve().body(new ParameterizedTypeReference<>(){});
        if(d==null||!id.equals(d.get("public_id"))||!"image".equals(d.get("resource_type"))||!String.valueOf(d.get("secure_url")).startsWith("https://res.cloudinary.com/"+properties.getCloudName()+"/image/upload/"))throw new IllegalArgumentException();
        return new ImageMetadata(String.valueOf(d.get("secure_url")),String.valueOf(d.get("format")),((Number)d.get("bytes")).longValue(),((Number)d.get("width")).intValue(),((Number)d.get("height")).intValue());
    }catch(RuntimeException e){throw CloudinaryFailures.failure("VERIFICAR_IMAGEN",e);}}
    @Override public void delete(String id){configured();try{
        Map<String,String> p=Map.of("public_id",id,"timestamp",Long.toString(Instant.now().getEpochSecond()),"invalidate","true");var body=new LinkedMultiValueMap<String,String>();p.forEach(body::add);body.add("api_key",properties.getApiKey());body.add("signature",sign(p));
        Map<String,Object>d=client.post().uri("/{cloud}/image/destroy",properties.getCloudName()).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body).retrieve().body(new ParameterizedTypeReference<>(){});
        if(d==null||!Set.of("ok","not found").contains(String.valueOf(d.get("result"))))throw new IllegalArgumentException();
    }catch(RuntimeException e){throw CloudinaryFailures.failure("ELIMINAR_IMAGEN",e);}}
}
