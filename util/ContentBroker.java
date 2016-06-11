package util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Transforme une chaine JSON en map clé, valeur
 * et vice et versa
 * @author Claude Moulin
 */
public class ContentBroker {
  Map<String,Object> map;
  ObjectMapper  mapper;
  public ContentBroker() {
	  map = new HashMap<String,Object>();
	  mapper  =  new  ObjectMapper();
  }
  public ContentBroker(String content) {
	  mapper  =  new  ObjectMapper();
	  parse(content);
  }
  public ContentBroker(Map<String,Object> map) {
	  this.map = map;
	  mapper  =  new  ObjectMapper();
  }
  public void put(String key,Object value) {
	  map.put(key, value);
  }
  public String get(String key) {
	  return (String) map.get(key);
  }
  public Map<String,Object> getMap() {
	  return map;
  }
  private void parse(String content) {
	try {
		map  =  mapper.readValue(content,  Map.class);
	} catch (IOException e) {
		e.printStackTrace();
	}
  }
  public String toJson() {
	StringWriter sw = new StringWriter();
	try {
		mapper.writeValue(sw, map);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  return sw.toString();
  }
}
