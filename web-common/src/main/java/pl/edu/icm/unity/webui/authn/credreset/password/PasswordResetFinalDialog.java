/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFinalDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 6th, last step of credential reset pipeline. In this dialog the user must provide the new credential.
 * 
 * @author P. Piernik
 */
public class PasswordResetFinalDialog extends CredentialResetFinalDialog
{

	public PasswordResetFinalDialog(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor)
	{
		super(msg, backend, credEditor, 5);
	}
	
}
