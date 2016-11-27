/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Implements triggering of user import - performs authz and delegates to the internal service.
 * @author K. Benedyczak
 */
@Component
public class UserImportManagementImpl implements UserImportManagement
{
	private AuthorizationManager authz;
	private UserImportSerivce importService;
	
	@Autowired
	public UserImportManagementImpl(AuthorizationManager authz, UserImportSerivce importService)
	{
		this.authz = authz;
		this.importService = importService;
	}


	@Override
	public AuthenticationResult importUser(String identity, String type) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return importService.importUser(identity, type);
	}
}