/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.webhook;

import java.util.Map;

import org.apache.http.HttpResponse;

import pl.edu.icm.unity.engine.api.integration.Webhook;
import pl.edu.icm.unity.exceptions.EngineException;

public interface WebhookProcessor
{
	HttpResponse trigger(Webhook webhook, Map<String, String> params) throws EngineException;
}
