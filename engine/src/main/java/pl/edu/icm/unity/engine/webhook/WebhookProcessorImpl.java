/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.webhook;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventRegistry;
import pl.edu.icm.unity.engine.api.integration.Webhook;
import pl.edu.icm.unity.engine.api.integration.Webhook.WebhookHttpMethod;
import pl.edu.icm.unity.engine.api.webhook.WebhookProcessor;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
public class WebhookProcessorImpl implements WebhookProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, WebhookProcessorImpl.class);
	private PKIManagement pkiMan;

	@Autowired
	public WebhookProcessorImpl(PKIManagement pkiMan, IntegrationEventRegistry integrationEventRegistry)
	{
		this.pkiMan = pkiMan;
	}

	@Override
	public HttpResponse trigger(Webhook webhook, Map<String, String> params) throws EngineException
	{
		try
		{
			if (webhook.httpMethod.equals(WebhookHttpMethod.GET))
			{
				return doGet(webhook, params);

			} else
			{
				return sendPost(webhook, params);
			}
		} catch (Exception e)
		{
			log.error("Can not execute webhook", e);
			throw new EngineException(e.getMessage(), e);
		}
	}

	private HttpResponse doGet(Webhook webhook, Map<String, String> params)
			throws ClientProtocolException, IOException, EngineException, URISyntaxException
	{
		URIBuilder urlBuilder = new URIBuilder(webhook.url);
		params.forEach((k, v) -> {
				urlBuilder.addParameter(k, v);	
		});
		URL url = urlBuilder.build().toURL();
		HttpClient httpClient = getSSLClient(url.toString(), webhook.truststore);
		HttpGet request = new HttpGet(url.toString());
		log.debug("Request GET to " + url.toString());
		HttpResponse response = httpClient.execute(request);
		return response;
	}

	private HttpResponse sendPost(Webhook webhook, Map<String, String> params)
			throws ClientProtocolException, IOException, EngineException
	{
		HttpPost post = new HttpPost(webhook.url);
		List<NameValuePair> urlParameters = new ArrayList<>();
		params.forEach((k, v) -> {
				urlParameters.add(new BasicNameValuePair(k, v));
		});
		post.setEntity(new UrlEncodedFormEntity(urlParameters));
		HttpClient httpClient = getSSLClient(webhook.url, webhook.truststore);
		log.debug("Request POST to " + webhook.url);
		HttpResponse response = httpClient.execute(post);
		return response;
	}

	private HttpClient getSSLClient(String url, String customTruststore) throws EngineException
	{
		if (customTruststore != null)
		{
			DefaultClientConfiguration config = new DefaultClientConfiguration();
			config.setSslEnabled(true);
			config.setValidator(pkiMan.getValidator(customTruststore));
			return HttpUtils.createClient(url, config);
		} else
		{
			return HttpClientBuilder.create().build();
		}
	}
}
