/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

/**
 * Single credential editor with credential extra info
 * @author P.Piernik
 *
 */
public class SingleCredentialPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialsPanel.class);
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private UnityMessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private final boolean simpleMode;
	private final boolean showButtons;
	private boolean askAboutCurrent;	
	private HtmlConfigurableLabel credentialName;
	private VerticalLayout credentialStatus;
	private Button update;
	private Button clear;
	private Button invalidate;
	private CredentialEditor credEditor;
	private ComponentsContainer credEditorComp;
	private CredentialDefinition toEdit;
	
	
	public SingleCredentialPanel(UnityMessageSource msg, long entityId,
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialEditorRegistry credEditorReg, CredentialDefinition toEdit,
			boolean simpleMode, boolean showButtons) throws Exception
	{
		this.msg = msg;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.simpleMode = simpleMode;
		this.showButtons = showButtons;
		this.toEdit = toEdit;
		loadEntity(new EntityParam(entityId));
		init();
	}

	private void init() throws Exception
	{
		credentialName = new HtmlConfigurableLabel();
		credentialName.setCaption(msg.getMessage("CredentialChangeDialog.credentialName"));
		credentialStatus = new VerticalLayout();
		credentialStatus.setMargin(false);
		credentialStatus.setCaption(
				msg.getMessage("CredentialChangeDialog.credentialStateInfo"));

		credEditor = credEditorReg.getEditor(toEdit.getTypeId());
		askAboutCurrent = isCurrentCredentialVerificationRequired(toEdit);

		credEditorComp = credEditor.getEditor(askAboutCurrent,
				toEdit.getConfiguration(), true, entityId, !simpleMode);

		clear = new Button(msg.getMessage("CredentialChangeDialog.clear"));
		clear.setIcon(Images.undeploy.getResource());
		clear.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				changeCredentialStatus(LocalCredentialState.notSet);
			}
		});

		invalidate = new Button(msg.getMessage("CredentialChangeDialog.invalidate"));
		invalidate.setIcon(Images.warn.getResource());
		invalidate.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				changeCredentialStatus(LocalCredentialState.outdated);
			}
		});

		update = new Button(msg.getMessage("CredentialChangeDialog.update"));
		update.setIcon(Images.save.getResource());
		update.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				updateCredential(true);
			}
		});

		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);

		if (showButtons)
		{
			if (!simpleMode)
			{
				buttonsBar.addComponent(clear);
				buttonsBar.addComponent(invalidate);
			}
			buttonsBar.addComponent(update);
		}

		FormLayout fl = new CompactFormLayout(credentialName, credentialStatus);
		fl.setMargin(true);
		addComponent(fl);
		if (!isEmptyEditor())
		{
			fl.addComponent(new Label());
			fl.addComponents(credEditorComp.getComponents());
			addComponent(buttonsBar);
		}

		setSpacing(true);
		setMargin(false);
		updateCredentialStatus();
	}

	public boolean isChanged()
	{
		return changed;
	}

	public boolean isEmptyEditor()
	{
		return credEditorComp.getComponents().length == 0;
	}
	
	private String getStatusIcon(LocalCredentialState state)
	{
		if (state.equals(LocalCredentialState.correct))
			return Images.ok.getHtml();
		else if (state.equals(LocalCredentialState.notSet))
			return Images.undeploy.getHtml();
		else
			return Images.warn.getHtml();
	}

	private void updateCredentialStatus()
	{

		String desc = toEdit.getDescription().getValue(msg);
		if (desc != null && !desc.isEmpty())
		{
			credentialName.setValue(desc);
		} else
		{
			credentialName.setValue(toEdit.getName());
		}

		Map<String, CredentialPublicInformation> s = entity.getCredentialInfo()
				.getCredentialsState();
		CredentialPublicInformation credPublicInfo = s.get(toEdit.getName());

		credentialStatus.removeAllComponents();
		Label status = new Label(getStatusIcon(credPublicInfo.getState()) + " "
				+ msg.getMessage("CredentialStatus."
						+ credPublicInfo.getState().toString()));
		status.setContentMode(ContentMode.HTML);
		credentialStatus.addComponent(status);
		ComponentsContainer viewer = credEditor
				.getViewer(credPublicInfo.getExtraInformation());

		if (viewer == null)
		{
			credentialStatus.setVisible(false);
		} else
		{
			credentialStatus.addComponents(viewer.getComponents());
			credentialStatus.setVisible(true);
		}
		if (credPublicInfo.getState() == LocalCredentialState.notSet)
		{
			clear.setEnabled(false);
			invalidate.setEnabled(false);
		} else if (credPublicInfo.getState() == LocalCredentialState.outdated)
		{
			clear.setEnabled(true);
			invalidate.setEnabled(false);
		} else
		{
			clear.setEnabled(true);
			invalidate.setEnabled(true);
		}
	}

	private boolean isCurrentCredentialVerificationRequired(CredentialDefinition chosen)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			return ecredMan.isCurrentCredentialRequiredForChange(entityP,
					chosen.getName());
		} catch (EngineException e)
		{
			log.debug("Got exception when asking about possibility to "
					+ "change the credential without providing the existing one."
					+ " Most probably the subsequent credential change will also fail.",
					e);
			return true;
		}
	}

	public boolean updateCredential(boolean showSuccess)
	{
		String secrets, currentSecrets = null;
		try
		{
			if (askAboutCurrent)
				currentSecrets = credEditor.getCurrentValue();
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			NotificationPopup.showError(msg, msg
					.getMessage("CredentialChangeDialog.credentialUpdateError"),
					e.getMessage());
			return false;
		}
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			if (askAboutCurrent)
				ecredMan.setEntityCredential(entityP, toEdit.getName(), secrets,
						currentSecrets);
			else
				ecredMan.setEntityCredential(entityP, toEdit.getName(), secrets);
		} catch (IllegalCredentialException e)
		{
			credEditor.setCredentialError(e);
			return false;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e);
			return false;
		}
		credEditor.setCredentialError(null);
		if (showSuccess)
			NotificationPopup.showSuccess(msg,
					msg.getMessage("CredentialChangeDialog.credentialUpdated"),
					"");
		changed = true;
		loadEntity(entityP);
		updateCredentialStatus();
		return true;
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredentialStatus(entityP, toEdit.getName(), desiredState);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e);
			return;
		}
		loadEntity(entityP);
		updateCredentialStatus();
	}

	private void loadEntity(EntityParam entityP)
	{
		try
		{
			entity = entityMan.getEntity(entityP);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("CredentialChangeDialog.entityRefreshError"),
					e);
		}
	}

}
