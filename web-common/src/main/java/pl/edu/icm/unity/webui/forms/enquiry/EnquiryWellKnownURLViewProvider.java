/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.TopHeader;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryWellKnownURLView.Callback;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.wellknownurl.SecuredViewProvider;

/**
 * Standalone view presenting enquiry form.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryWellKnownURLViewProvider implements SecuredViewProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EnquiryWellKnownURLViewProvider.class);
	@Autowired
	private EnquiryResponseEditorController editorController;
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private StandardWebAuthenticationProcessor authnProcessor;
	
	/**
	 * @implNote: due to changes in the enquiry links, below format was kept for
	 *            backwards compatibility reasons.
	 */
	@Deprecated
	private static final String ENQUIRY_FRAGMENT_PREFIX = "enquiry-";
	
	@Override
	public String getViewName(String viewAndParameters)
	{
		String formName = getFormName(viewAndParameters);
		if (formName == null)
			return null;
		
		EnquiryForm enquiry = editorController.getForm(formName);
		if (enquiry == null)
			return null;
		
		return viewAndParameters;
	}
	
	@Override
	public View getView(String viewName)
	{
		String formName = getFormName(viewName);
		if (!editorController.isFormApplicable(formName))
			return new NotApplicableView();
		
		EnquiryForm form = editorController.getForm(formName);
		EnquiryResponseEditor editor;
		try
		{
			editor = editorController.getEditorInstance(form, 
					RemotelyAuthenticatedContext.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can't load enquiry editor", e);
			return null;
		}
		
		return new EnquiryWellKnownURLView(editor, authnProcessor, msg, new Callback()
		{
			@Override
			public boolean submitted()
			{
				EnquiryResponse request;
				try
				{
					request = editor.getRequest(true);
				} catch (Exception e)
				{
					NotificationPopup.showError(msg, 
							msg.getMessage("EnquiryResponse.errorSubmit"), e);
					return false;
				}
				
				try
				{
					return editorController.submitted(request, form, TriggeringMode.manualStandalone);
				} catch (WrongArgumentException e)
				{
					NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
					if (e instanceof IllegalFormContentsException)
						editor.markErrorsFromException((IllegalFormContentsException) e);
					return false;
				}
			}
			
			@Override
			public void cancelled()
			{
				editorController.cancelled(form, TriggeringMode.manualStandalone);
			}
		});
	}
	
	private String getFormName(String viewAndParameters)
	{
		if (PublicRegistrationURLSupport.ENQUIRY_VIEW.equals(viewAndParameters))
			return RegistrationFormDialogProvider.getFormFromURL();
		
		if (viewAndParameters.startsWith(ENQUIRY_FRAGMENT_PREFIX))
			return viewAndParameters.substring(ENQUIRY_FRAGMENT_PREFIX.length());
		
		return null;
	}

	@Override
	public void setEndpointConfiguration(Properties configuration)
	{
	}

	@Override
	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier,
			String sandboxUrlForAssociation)
	{
	}
	
	private class NotApplicableView extends CustomComponent implements View
	{

		@Override
		public void enter(ViewChangeEvent event)
		{
			VerticalLayout wrapper = new VerticalLayout();
			
			TopHeader header = new TopHeader("", authnProcessor, msg);
			wrapper.addComponent(header);
			
			ConfirmationComponent confirmation = new ConfirmationComponent(Images.error, 
					msg.getMessage("EnquiryWellKnownURLViewProvider.notApplicableEnquiry"));
			wrapper.addComponent(confirmation);
			wrapper.setComponentAlignment(confirmation, Alignment.MIDDLE_CENTER);
			wrapper.setExpandRatio(confirmation, 2f);
			wrapper.setSizeFull();
			wrapper.setSpacing(false);
			wrapper.setMargin(false);
			setSizeFull();
			setCompositionRoot(wrapper);
		}
	}
}
