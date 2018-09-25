/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.example;

import static org.junit.Assert.assertNotNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadTimeoutTest {
  private static final Logger LOG = LoggerFactory.getLogger(RetryHttpRequestInitializer.class);

  private static String TOXIPROXY_HOST = "127.0.0.1";
  static {
    String host = System.getenv("PROXY_HOST");
    if (host != null) {
      TOXIPROXY_HOST = host;
    }
  }
  private final static ToxiproxyClient TOXIPROXY = new ToxiproxyClient(TOXIPROXY_HOST, 8474);
  private static String UPSTREAM_HOST = "127.0.0.1";
  static {
    String host = System.getenv("UPSTREAM_HOST");
    if (host != null) {
      UPSTREAM_HOST = host;
    }
  }

  @Before
  public void setUp() throws IOException {
    TOXIPROXY.reset();
  }

  @Test(timeout=100000)
  public void testBrokenUpload() throws IOException {
    Proxy p = findOrCreateProxy("http", TOXIPROXY_HOST + ":21212", UPSTREAM_HOST + ":8080");
    p.toxics().limitData("limitUp", ToxicDirection.UPSTREAM, 1);
    p.toxics().slowClose("slowCloseDown", ToxicDirection.DOWNSTREAM, 10 * 1000 * 1000);

    HttpRequestInitializer requestInitializer = new RetryHttpRequestInitializer();
    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(requestInitializer);

    InputStream is = getClass().getClassLoader().getResourceAsStream("file.txt");
    LOG.debug("input stream {}", is);
    HttpContent content = new InputStreamContent("text/plain", is);
    HttpRequest request = requestFactory.buildPostRequest(new GenericUrl("http://" + p.getListen()), content);

    LOG.debug("making request");
    String response = request.execute().parseAsString();
    assertNotNull(response);
  }

  private Proxy findOrCreateProxy(String name, String listen, String upstream) throws IOException {
    Proxy p = TOXIPROXY.getProxyOrNull(name);
    if (p == null) {
      p = TOXIPROXY.createProxy(name, listen, upstream);
    }
    return p;
  }

}
