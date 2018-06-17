/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.unity.composite.password;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Utility class with methods used by {@link CompositePasswordVerificator} and {@link CompositePasswordResetImpl}
 * @author P.Piernik
 *
 */
public class CompositePasswordHelper
{
	public static boolean checkCredential(LocalCredentialVerificator verificator, long entityId)
	{
		try
		{
			return verificator.isCredentialSet(new EntityParam(entityId));

		} catch (EngineException e)
		{

			throw new InternalException("Can not check if user have local credential",
					e);
		}
	}

	public static Optional<EntityWithCredential> getLocalEntity(
			IdentityResolver identityResolver, String username)
	{
		try
		{
			return Optional.of(identityResolver.resolveIdentity(username,
					CompositePasswordVerificator.IDENTITY_TYPES, null));
		} catch (IllegalIdentityValueException e)
		{

			// ok, maybe we have remote authn
		} catch (EngineException e)
		{
			throw new InternalException("Can not resolve username", e);
		}

		return Optional.empty();
	}

	public static Optional<CredentialDefinition> getCredentialDefinition(
			CredentialHelper credentialHelper, String credential)
	{
		try
		{
			return Optional.ofNullable(credentialHelper.getCredentialDefinitions()
					.get(credential));
		} catch (EngineException e)
		{
			throw new InternalException("Can not get credential definitions", e);
		}
	}

}
