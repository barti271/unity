/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * View for add realm
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class NewRealm extends AbstractEditRealm
{

	@Autowired
	public NewRealm(UnityMessageSource msg, RealmController controller)
	{
		super(msg, controller);
	}

	@Override
	protected void onConfirm()
	{
		if (binder.validate().hasErrors())
		{
			return;
		}

		try
		{
			if (!controller.addRealm(binder.getBean()))
				return;
		} catch (Exception e)
		{
			// TODO
			NotificationPopup.showError(msg, "IVALID REALM", e);
			return;
		}

		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());

	}

	@Override
	protected void onCancel()
	{
		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());

	}

	@Override
	protected void init(Map<String, String> parameters)
	{
		AuthenticationRealm bean = new AuthenticationRealm();
		bean.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		bean.setAllowForRememberMeDays(14);
		bean.setBlockFor(60);
		bean.setMaxInactivity(1800);
		bean.setBlockAfterUnsuccessfulLogins(5);
		binder.setBean(bean);
	}
}
