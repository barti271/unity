/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 2nd step of credential reset pipeline. In this dialog the user must provide an answer to the security question.
 * In future other attributes might be queried here.
 * <p>
 * This dialog fails if either username or the answer is wrong. This is done to make guessing usernames 
 * more difficult. In future, with other attribute queries it will be even more bullet proof.
 * In case the user is invalid, we present a 'random' question. However we must be sure that for the given 
 * username always the same question is asked, so our choices are not random.
 * <p>
 * This check is intended before any confirmation code sending, not to spam users.
 * 
 * @author K. Benedyczak
 */
public class PasswordCredentialReset2Dialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	private String username;
	private CredentialEditor credEditor;
	private PasswordCredentialResetSettings settings;
	
	private TextField answer;
	
	public PasswordCredentialReset2Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor, 
			String username)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("continue"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.username = username;
		this.credEditor = credEditor;
		this.settings = new PasswordCredentialResetSettings(
				JsonUtil.parse(backend.getSettings()));
		setSizeEm(40, 30);
	}

	@Override
	protected Component getContents() throws Exception
	{
		addStyleName("u-credreset-dialog");

		if (CredentialResetStateVariable.get() != 1)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		Label userLabel = new Label(msg.getMessage("CredentialReset.changingFor", username));
		
		Label question = new Label(backend.getSecurityQuestion());
		question.setCaption(msg.getMessage("CredentialReset.question"));
		answer = new TextField(msg.getMessage("CredentialReset.answer"));
		
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.addComponent(userLabel);
		VerticalLayout form = new VerticalLayout(question, answer);
		ret.addComponent(form);
		return ret;
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
		if (CredentialResetStateVariable.get() != 1)
			throw new IllegalStateException("Wrong application security state in password reset!" +
					" This should never happen.");

		String a = answer.getValue();
		if (a == null || a.equals(""))
		{
			answer.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		answer.setComponentError(null);

		try
		{
			backend.verifyStaticData(a);
		} catch (TooManyAttempts e) 
		{
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			onCancel();
			return;
		} catch (Exception e)
		{
			answer.setValue("");
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			return;
		}
		
		close();

		CredentialResetStateVariable.inc(); // ok - next step allowed
		if (settings.isRequireEmailConfirmation())
		{
			CredentialResetStateVariable.inc();
			EmailCodePasswordCredentialReset4Dialog dialog4 = new EmailCodePasswordCredentialReset4Dialog(
					msg, backend, credEditor, username);
			dialog4.show();
		} else if (settings.isRequireMobileConfirmation())

		{
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			MobileCodePasswordCredentialReset5Dialog dialog5 = new MobileCodePasswordCredentialReset5Dialog(
					msg, backend, credEditor, username);
			dialog5.show();

		} else if (settings.getConfirmationMode()
				.equals(ConfirmationMode.RequireEmailOrMobile))

		{
			PasswordCredentialResetVerificationChoose3Dialog dialog3 = new PasswordCredentialResetVerificationChoose3Dialog(
					msg, backend, credEditor, username);
			dialog3.show();
		}

		else
		{
			// nothing more required, jump to final step 6
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			PasswordResetFinalDialog dialogFinal = new PasswordResetFinalDialog(msg,
					backend, credEditor);
			dialogFinal.show();
		}
	}
	
}
