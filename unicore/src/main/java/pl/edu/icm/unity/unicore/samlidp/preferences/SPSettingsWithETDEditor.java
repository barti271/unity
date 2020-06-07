/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.saml.idp.preferences.SPSettingsEditor;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.web.ETDSettingsEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Allows to edit settings for a single UNICORE SAML service provider.
 * Implementation wise it merges {@link SPSettingsEditor} with {@link ETDSettingsEditor}
 * 
 * @author K. Benedyczak
 */
public class SPSettingsWithETDEditor extends SPSettingsEditor
{
	private ETDSettingsEditor editor;
	
	public SPSettingsWithETDEditor(MessageSource msg, AttributeHandlerRegistry attributeHandlerRegistries,
			IdentityTypeSupport idTypeSupport, List<Identity> identities, 
			Collection<AttributeType> atTypes, String sp, SPSettings initial, SPETDSettings initialETD)
	{
		super(msg, attributeHandlerRegistries, idTypeSupport, identities, atTypes, sp, initial);
		editor = new ETDSettingsEditor(msg, this);
		editor.setValues(initialETD);
	}

	public SPSettingsWithETDEditor(MessageSource msg, AttributeHandlerRegistry attributeHandlerRegistries, 
			IdentityTypeSupport idTypeSupport, List<Identity> identities, Collection<AttributeType> atTypes,
			Set<String> allSps)
	{
		super(msg, attributeHandlerRegistries, idTypeSupport, identities, atTypes, allSps);
		editor = new ETDSettingsEditor(msg, this);
	}
	
	public SPETDSettings getSPETDSettings()
	{
		return editor.getSPETDSettings();
	}
	
	/**
	 * In UNICORE case the service provider should be given as DN. 
	 * Return comparable form if possible
	 */
	@Override
	public String getSP()
	{
		if (sp == null)
			return spLabel.getValue();
		String spStr = (String) sp.getValue();
		
		try
		{
			return X500NameUtils.getComparableForm(spStr);
		} catch (Exception e)
		{
			return spStr;
		}
	}
}
