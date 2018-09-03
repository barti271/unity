/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Management of registration invitations.
 * @author Krzysztof Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InvitationsComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationsComponent.class);

	private UnityMessageSource msg;
	private RegistrationsManagement registrationManagement;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private IdentityEditorRegistry identityEditorRegistry;
	private MessageTemplateManagement msgTemplateManagement;

	private AttributeTypeManagement attributesManagement;

	private InvitationManagement invitationManagement;

	private GroupsManagement groupsManagement;
	
	@Autowired
	public InvitationsComponent(UnityMessageSource msg,
			RegistrationsManagement registrationManagement,
			AttributeTypeManagement attributesManagement,
			InvitationManagement invitationManagement,
			AttributeHandlerRegistry attrHandlersRegistry,
			IdentityEditorRegistry identityEditorRegistry,
			MessageTemplateManagement msgTemplateManagement,
			GroupsManagement groupsManagement)
	{
		this.msg = msg;
		this.registrationManagement = registrationManagement;
		this.attributesManagement = attributesManagement;
		this.invitationManagement = invitationManagement;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.identityEditorRegistry = identityEditorRegistry;
		this.msgTemplateManagement = msgTemplateManagement;
		this.groupsManagement = groupsManagement;
		initUI();
	}

	private void initUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		InvitationsTable invitationsTable = new InvitationsTable(msg,
				registrationManagement, invitationManagement, attributesManagement,
				identityEditorRegistry, attrHandlersRegistry,
				msgTemplateManagement, groupsManagement);
		InvitationViewer viewer = new InvitationViewer(msg, attrHandlersRegistry,
				msgTemplateManagement, registrationManagement);

		invitationsTable.addValueChangeListener(invitation -> 
			viewer.setInput(invitation, getForm(invitation))
		);
		
		CompositeSplitPanel hl = new CompositeSplitPanel(false, true, invitationsTable, viewer, 40);
		setCompositionRoot(hl);
		setCaption(msg.getMessage("InvitationsComponent.caption"));
	}

	private RegistrationForm getForm(InvitationWithCode invitation)
	{
		if (invitation == null)
			return null;
		List<RegistrationForm> forms;
		try
		{
			forms = registrationManagement.getForms();
		} catch (EngineException e)
		{
			log.warn("Unable to list registration forms for invitations", e);
			return null;
		}
		String id = invitation.getFormId();
		Optional<RegistrationForm> found = forms.stream().filter(form -> form.getName().equals(id)).findAny();
		if (found.isPresent())
			return found.get();
		return null;
	}
}
