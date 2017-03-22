/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.cominvent.solr;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * SearchComponent plugin for Solr 6.x. Will probably also work for 5.x
 * but needs Java 8 installed.
 * <p><b>Usage:</b></p>
 * <p>Add the <code>sanitize</code> request parameter in solrconfig.xml. Examples:<br/>
 * <dl>
 *   <dt>Always override the field, just like invariant</dt>
 *   <dd><code>sanitize=rows=25</code> or <code>sanitize=rows=invariant:25</code></dd>
 *   <dt>Map values to other values (if no match found, will use input value</dt>
 *   <dd><code>sanitize=echoParams=alle:all eksplisitt:explicit</code></dd>
 *   <dt>Set default value if param is not set</dt>
 *   <dd><code>sanitize=debugQuery=default:true</code></dd>
 *   <dt>Restrict numeric value to a max limit (if >100 then cap at 100)</dt>
 *   <dd><code>sanitize=rows=>100:100</code></dd>
 *   <dt>Multiple replacements through multiple http params</dt>
 *   <dd><code>sanitize=rows=>100:100&sanitize=offset=>10000:10000</code></dd>
 * </dl>
 * </p>
*/
public class RequestSanitizerComponent extends SearchComponent
{
  /** The component name */
  static final public String COMPONENT_NAME = "request_sanitizer";
  private static final Logger log = LoggerFactory.getLogger(RequestSanitizerComponent.class);

  private static final String SANITIZE_PARAM = "sanitize";

  private List<String> replacements;

  public RequestSanitizerComponent()
  {
    super();
  }

  @Override
  public void init(NamedList args)
  {
    super.init(args);
    log.info("Initialized RequestSanitizerComponent.");
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException
  {
    SolrParams params = rb.req.getParams();
    if (!params.getBool(COMPONENT_NAME, true))
      return;
    Map<String, Map<String, String>> mappings = parseMappings(Arrays.asList(params.getParams(SANITIZE_PARAM)));
    log.info("Initialized RequestSanitizerComponent with mappings " + mappings);

    Map<String, String> modified = getModifications(params, mappings);
    if (modified.size() > 0) {
      log.info("Request parameters that were modified by RequestSanitizerComponent: " + modified);
      rb.req.setParams(new DefaultSolrParams(new MapSolrParams(modified), params));
    }
  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
  }

  /**
   * Parses the mapping parameters
   * @param replacements a list of all sanitize http params to parse
   * @return a Map of parsed mappings that can be looked up
   */
  protected Map<String, Map<String, String>> parseMappings(List<String> replacements) {
    Map<String,Map<String,String>> parsedMappings = new HashMap<>();
    if (replacements == null || replacements.size() == 0) {
      return parsedMappings;
    }
    for (String replacement : replacements) {
      int offset = replacement.indexOf("=");
      if (offset == -1) {
        log.error("Parameter " + SANITIZE_PARAM + " must be on the format " + SANITIZE_PARAM + "=param=<value>");
        throw new SolrException(ErrorCode.BAD_REQUEST, "Parameter " + SANITIZE_PARAM + " must be on the format " + SANITIZE_PARAM + "=param=<value>");
      }
      String param = replacement.substring(0, offset);
      String val = replacement.substring(offset+1);
      if (!val.contains(":")) {
        parsedMappings.put(param, Collections.singletonMap("invariant", val));
        continue;
      } else {
        Map<String,String> keyVal = new HashMap<>();
        String[] rules = val.split(" ");
        for (String rule : rules) {
          String[] kv = rule.split(":");
          if (kv.length != 2) {
            throw new SolrException(ErrorCode.BAD_REQUEST, "Parameter " + SANITIZE_PARAM + " must be on the format " + SANITIZE_PARAM + "=param=from:to from:to default:val >num:num2");
          }
          keyVal.put(kv[0], kv[1]);
        }
        parsedMappings.put(param, keyVal);
      }
    }
    return parsedMappings;
  }

  /**
   * Modify input params if mappings exist
   * @param origParams the set of input parameters for the request
   * @param mappings the mappings parsed from the config
   * @return a Map of override values which can be applied on top of the original params
   */
  protected Map<String, String> getModifications(SolrParams origParams, Map<String, Map<String, String>> mappings) {
    Map<String,String> params = new HashMap<String, String>();
    if (mappings == null) {
      throw new SolrException(ErrorCode.BAD_REQUEST, "mappings cannot be null");
    }

    for (String parToReplace : mappings.keySet()) {
      Map<String,String> keyVal = mappings.get(parToReplace);
      if (keyVal.get("invariant") != null) {
        params.put(parToReplace, keyVal.get("invariant"));
        log.debug("Param " + parToReplace + " not given in request, setting to invariant: " + keyVal.get("invariant"));
        continue;
      }
      if (origParams.get(parToReplace) == null) {
        if (keyVal.get("default") != null) {
          params.put(parToReplace, keyVal.get("default"));
          log.debug("Param " + parToReplace + " not given in request, setting to default: " + keyVal.get("default"));
        }
        continue;
      }

      String origVal = origParams.get(parToReplace);
      for (String k : keyVal.keySet()) {
        if (k.startsWith(">") || k.startsWith("<")) {
          int trueCondition = k.startsWith(">") ? 1 : -1;
          Long cmp;
          try {
            cmp = Long.parseLong(k.substring(1));
          } catch (NumberFormatException e) {
            log.error("Wrong format of replace rule for param " + parToReplace + ":" + k + ":" + keyVal.get(k));
            throw new SolrException(ErrorCode.BAD_REQUEST, "Wrong format of replace rule for param " + parToReplace + ":" + k + ":" + keyVal.get(k));
          }
          try {
            Long orig = Long.parseLong(origVal);
            if (orig.compareTo(cmp) == trueCondition || orig.compareTo(cmp) == 0) {
              log.debug("Param " + parToReplace + " hit rule " + k);
              params.put(parToReplace, keyVal.get(k));
              break;
            }
          } catch (NumberFormatException e) {
            log.debug("Target value is not a number, ignoring max test");
          }
        } else if (k.equals(origVal)) {
          log.debug("Replacing param " + parToReplace + "=" + origVal + "=>" + keyVal.get(k));
          params.put(parToReplace, keyVal.get(k));
          break;
        }
      }
    }
    return params;
  }


  //---------------------------------------------------------------------------------
  // SolrInfoMBean
  //---------------------------------------------------------------------------------
  @Override
  public String getDescription()
  {
    return "RequestSanitizerComponent";
  }

  @Override
  public String getVersion()
  {
    return "1.0";
  }

  public class DefaultSolrParams extends SolrParams {

    protected final SolrParams params;
    protected final SolrParams defaults;

    public DefaultSolrParams(SolrParams params, SolrParams defaults) {
      assert params != null && defaults != null;
      this.params = params;
      this.defaults = defaults;
    }

    @Override
    public String get(String param) {
      String val = params.get(param);
      return val!=null ? val : defaults.get(param);
    }

    @Override
    public String[] getParams(String param) {
      String[] vals = params.getParams(param);
      return vals!=null ? vals : defaults.getParams(param);
    }

    @Override
    public Iterator<String> getParameterNamesIterator() {
      // We need to compute the set of all param names in advance
      // So we don't wind up with an iterator that returns the same
      // String more then once (SOLR-6780)
      LinkedHashSet<String> allKeys = new LinkedHashSet<>();
      for (SolrParams p : new SolrParams [] {params, defaults}) {
        Iterator<String> localKeys = p.getParameterNamesIterator();
        while (localKeys.hasNext()) {
          allKeys.add(localKeys.next());
        }
      }
      return allKeys.iterator();
    }

    @Override
    public String toString() {
      return "{params("+params+"),defaults("+defaults+")}";
    }
  }


}
