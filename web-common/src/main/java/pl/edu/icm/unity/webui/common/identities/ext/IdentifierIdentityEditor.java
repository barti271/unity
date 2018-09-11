/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import com.vaadin.server.UserError;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;

/**
 * {@link IdentifierIdentity} editor
 * @author K. Benedyczak
 */
public class IdentifierIdentityEditor implements IdentityEditor
{
	private UnityMessageSource msg;
	private TextField field;
	private IdentityEditorContext context;
	
	public IdentifierIdentityEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		this.context = context;
		field = new TextField();
		setLabel(new IdentifierIdentity().getHumanFriendlyName(msg));
		field.setRequiredIndicatorVisible(context.isRequired());
		return new ComponentsContainer(field);
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		String username = field.getValue();
		if (username.trim().equals(""))
		{
			if (!context.isRequired())
				return null;
			String err = msg.getMessage("IdentifierIdentityEditor.errorEmpty");
			field.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		field.setComponentError(null);		
		return new IdentityParam(IdentifierIdentity.ID, username);
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		field.setValue(value.getValue());	
	}
	
	@Override
	public void setLabel(String value)
	{
		if (context.isShowLabelInline())
			field.setPlaceholder(value);
		else
			field.setCaption(value + ":");
	}

}
