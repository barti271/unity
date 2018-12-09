/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_7;


import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Shared code updating authenticators. 
 */
class UpdateHelperFrom2_7
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperFrom2_7.class);
	
	static ObjectNode updateAuthenticator(ObjectNode authenticator)
	{
		log.info("Updating authenticator from: \n{}", JsonUtil.toJsonString(authenticator));
		String name = authenticator.get("id").asText();
		String verificationMethod = authenticator.get("typeDescription").get("verificationMethod").asText();
		String configuration = authenticator.hasNonNull("verificatorConfiguration") ?
				authenticator.get("verificatorConfiguration").asText() : null;
		String localCredentialName = authenticator.hasNonNull("localCredentialName") ? 
				authenticator.get("localCredentialName").asText() : null;
		long revision = authenticator.hasNonNull("revision") ? 
				authenticator.get("revision").asLong() : 0;
		
		ObjectNode authenticatorConfig = Constants.MAPPER.createObjectNode();
		authenticatorConfig.put("name", name);
		authenticatorConfig.put("verificationMethod", verificationMethod);
		if (configuration != null)
			authenticatorConfig.put("configuration", configuration);
		if (localCredentialName != null)
			authenticatorConfig.put("localCredentialName", localCredentialName);
		authenticatorConfig.put("revision", revision);
		
		log.info("Updated authenticator to: \n{}", JsonUtil.toJsonString(authenticatorConfig));
		return authenticatorConfig;
	}
}