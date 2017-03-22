package com.cominvent.solr;

import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by janhoy on 22.03.2017.
 */
public class RequestSanitizerComponentTest {
  private RequestSanitizerComponent comp;
  private ArrayList<String> replacements;
  private Map<String, Map<String, String>> mappings;

  @Before
  public void before() {
    comp = new RequestSanitizerComponent();

    replacements = new ArrayList<String>();
    replacements.add("set=123");
    replacements.add("max=>1000:1000");
    replacements.add("case=foo:bar from:to");
    replacements.add("invar=invariant:myinvariant");
    replacements.add("def=default:mydefault");
    replacements.add("multi=0:1 >10:10 default:5");
    mappings = comp.parseMappings(replacements);
  }

  @Test
  public void parseMappings() throws Exception {
    assertEquals(6, mappings.size());
    assertEquals("123", mappings.get("set").get("invariant"));
    assertEquals("1000", mappings.get("max").get(">1000"));
    assertEquals("bar", mappings.get("case").get("foo"));
    assertEquals("10", mappings.get("multi").get(">10"));
  }

  @Test
  public void getModifications() throws Exception {
    verifyMapped("set", "5", "123", "multi", "5");
    verifyMapped("max", "100", null);
    verifyMapped("max", "2000", "1000");
    verifyMapped("case", null, null);
    verifyMapped("case", "foo", "bar");
    verifyMapped("case", "hey", null);
    verifyMapped("invar", "hello", "myinvariant");
    verifyMapped("def", null, "mydefault");
    verifyMapped("def", "something", null);
    verifyMapped("multi", null, "5");
    verifyMapped("multi", "0", "1");
    verifyMapped("multi", "2", null);
    verifyMapped("multi", "20", "10");
    verifyMapped("multi", "text", null);
  }

  private void verifyMapped(String param, String origVal, String afterVal, String... keyVals) {
    SolrParams orig = new MapSolrParams(Collections.singletonMap(param, origVal));
    Map<String, String> modified = comp.getModifications(orig, mappings);
    assertEquals(afterVal, modified.get(param));

    if (keyVals != null && keyVals.length > 0) {
      int offset = 0;
      while(keyVals.length > offset) {
        String key = keyVals[offset++];
        String val = keyVals[offset++];
        assertEquals(val, modified.get(key));
      }
    }
  }

}