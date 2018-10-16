/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Bootstraps credential reset pipeline.
 * @author K. Benedyczak
 */
public class PasswordCredentialReset1Dialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	
	private TextField username;
	private CaptchaComponent captcha;
	
	public PasswordCredentialReset1Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("CredentialReset.requestReset"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		setSizeEm(40, 30);
	}

	@Override
	protected Component getContents() throws Exception
	{
		addStyleName("u-credreset-dialog");
		if (CredentialResetStateVariable.get() != 0)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		
		Label info = new Label(msg.getMessage("CredentialReset.info"));
		info.setWidth(100, Unit.PERCENTAGE);
		main.addComponent(info);
		
		VerticalLayout centeredCol = new VerticalLayout();
		centeredCol.setMargin(false);
		centeredCol.setWidthUndefined();
		username = new TextField(msg.getMessage("CredentialReset.username"));
		centeredCol.addComponent(username);
		
		captcha = new CaptchaComponent(msg);
		Component captchaComp = captcha.getAsComponent(Alignment.TOP_LEFT);
		centeredCol.addComponent(captchaComp);

		main.addComponent(centeredCol);
		main.setComponentAlignment(centeredCol, Alignment.MIDDLE_CENTER);
		return main;
	}

	@Override
	protected void onCancel()
	{
		CredentialResetStateVariable.reset();
		super.onCancel();
	}
	
	@Override
	protected void onConfirm()
	{
		if (CredentialResetStateVariable.get() != 0)
			throw new IllegalStateException("Wrong application security state in password reset!" +
					" This should never happen.");
		String user = username.getValue();
		if (user == null || user.equals(""))
		{
			username.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		username.setComponentError(null);
		try
		{
			captcha.verify();
		} catch (WrongArgumentException e)
		{
			return;
		}

		close();
		backend.setSubject(new IdentityTaV(UsernameIdentity.ID, user));
		PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(JsonUtil.parse(backend.getSettings()));
		
		
		CredentialResetStateVariable.inc(); //ok - username (maybe invalid but anyway) and captcha are provided.
		//in future here we can also go to the 2nd dialog if other attributes are required.		
		if (settings.isRequireSecurityQuestion())
		{
			PasswordCredentialReset2Dialog dialog2 = new PasswordCredentialReset2Dialog(msg, backend,
					credEditor, user);
			dialog2.show();
		} else if (settings.isRequireEmailConfirmation())
		{
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			EmailCodePasswordCredentialReset4Dialog dialog4 = new EmailCodePasswordCredentialReset4Dialog(
					msg, backend, credEditor, user);
			dialog4.show();
		} else if (settings.isRequireMobileConfirmation())

		{
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			MobileCodePasswordCredentialReset5Dialog dialog5 = new MobileCodePasswordCredentialReset5Dialog(
					msg, backend, credEditor, user);
			dialog5.show();

		} else

		{
			CredentialResetStateVariable.inc();
			PasswordCredentialResetVerificationChoose3Dialog dialog3 = new PasswordCredentialResetVerificationChoose3Dialog(
					msg, backend, credEditor, user);
			dialog3.show();
		}
	}
	
}
