/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Read only UI displaying common parts of a {@link BaseForm}.
 * 
 * @author K. Benedyczak
 */
public class BaseFormViewer extends VerticalLayout
{
	private UnityMessageSource msg;
	
	protected Label name;
	protected Label description;
	
	protected I18nLabel displayedName;
	protected I18nLabel formInformation;
	protected Label collectComments;
	protected Label layout;
	private ListOfElements<AgreementRegistrationParam> agreements;	
	private ListOfElements<IdentityRegistrationParam> identityParams;
	private ListOfElements<AttributeRegistrationParam> attributeParams;
	private ListOfElements<GroupRegistrationParam> groupParams;
	private ListOfElements<CredentialRegistrationParam> credentialParams;

	private Panel credentialParamsP;

	private Panel groupParamsP;

	private Panel attributeParamsP;

	private Panel identityParamsP;

	private Panel agreementsP;

	private SafePanel localSignUpMethodsP;
	
	public BaseFormViewer(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	
	protected void setInput(BaseForm form)
	{
		if (form == null)
		{
			return;
		}
		
		name.setValue(form.getName());
		description.setValue(form.getDescription());
		
		displayedName.setValue(form.getDisplayedName());
		formInformation.setValue(form.getFormInformation());
		
		collectComments.setValue(msg.getYesNo(form.isCollectComments()));
		
		agreements.clearContents();
		identityParams.clearContents();
		attributeParams.clearContents();
		groupParams.clearContents();
		credentialParams.clearContents();
		for (IdentityRegistrationParam ip: form.getIdentityParams())
			identityParams.addEntry(ip);
		for (CredentialRegistrationParam cp: form.getCredentialParams())
			credentialParams.addEntry(cp);
		for (AttributeRegistrationParam ap: form.getAttributeParams())
			attributeParams.addEntry(ap);
		for (GroupRegistrationParam gp: form.getGroupParams())
			groupParams.addEntry(gp);
		for (AgreementRegistrationParam ap: form.getAgreements())
			agreements.addEntry(ap);
		
		agreementsP.setVisible(!form.getAgreements().isEmpty());
		identityParamsP.setVisible(!form.getIdentityParams().isEmpty());
		attributeParamsP.setVisible(!form.getAttributeParams().isEmpty());
		groupParamsP.setVisible(!form.getGroupParams().isEmpty());
		credentialParamsP.setVisible(!form.getCredentialParams().isEmpty());
		localSignUpMethodsP.setVisible(identityParamsP.isVisible() || credentialParamsP.isVisible());
		
		setSpacing(false);
		setMargin(false);
		setLayout(form);
	}
	
	protected void setLayout(BaseForm form)
	{
		StringBuilder info = new StringBuilder();
		if (form.getLayout() == null)
			info.append(msg.getMessage("RegistrationFormViewer.defaultLayout")).append("\n\n");
		for (FormElement formElement : form.getEffectiveFormLayout(msg).getElements())
			info.append(formElement.toString(msg)).append("\n");
		layout.setValue(info.toString());
	}
	
	protected void setupCommonFormInformationComponents()
	{
		displayedName = new I18nLabel(msg, msg.getMessage("RegistrationFormViewer.displayedName"));
		formInformation = new I18nLabel(msg, msg.getMessage("RegistrationFormViewer.formInformation"));
		collectComments = new Label();
		collectComments.setCaption(msg.getMessage("RegistrationFormViewer.collectComments"));
		layout = new Label();
		layout.setContentMode(ContentMode.PREFORMATTED);
		layout.setCaption(msg.getMessage("RegistrationFormViewer.layout"));
	}
	
	protected void setupNameAndDesc()
	{
		name = new Label();
		name.setCaption(msg.getMessage("RegistrationFormViewer.name"));
		
		description = new Label();
		description.setCaption(msg.getMessage("RegistrationFormViewer.description"));
	}
	
	protected Component getCollectedDataInformation()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(true);
		wrapper.setMargin(false);

		identityParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<IdentityRegistrationParam>()
		{
			@Override
			public Label toLabel(IdentityRegistrationParam value)
			{
				String content = msg.getMessage("RegistrationFormViewer.identityType", 
						value.getIdentityType());
				HtmlLabel ret = new HtmlLabel(msg);
				ret.setHtmlValue("RegistrationFormViewer.twoLines", content, toHTMLLabel(value));
				return ret;
			}
		});
		identityParams.setMargin(true);
		identityParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.identityParams"), identityParams);
		identityParamsP.setWidth(100, Unit.PERCENTAGE);
		
		credentialParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<CredentialRegistrationParam>()
		{
			@Override
			public Label toLabel(CredentialRegistrationParam value)
			{
				HtmlLabel ret = new HtmlLabel(msg);
				ret.setHtmlValue("RegistrationFormViewer.twoLines", value.getCredentialName(), 
						"[" + emptyNull(value.getLabel()) +  "] ["+
								emptyNull(value.getDescription()) + "]");
				return ret;
			}
		});
		credentialParams.setMargin(true);
		credentialParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.credentialParams"), 
				credentialParams);
		credentialParamsP.setWidth(100, Unit.PERCENTAGE);
		
		VerticalLayout localSignUpMethods = new VerticalLayout();
		localSignUpMethods.setWidth(100, Unit.PERCENTAGE);
		localSignUpMethods.addComponents(identityParamsP, credentialParamsP);
		localSignUpMethodsP = new SafePanel(msg.getMessage("RegistrationFormEditor.localSignupMethods"), localSignUpMethods);
		localSignUpMethodsP.setWidth(100, Unit.PERCENTAGE);
		
		agreements = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AgreementRegistrationParam>()
		{
			@Override
			public VerticalLayout toLabel(AgreementRegistrationParam value)
			{
				Label mandatory = new Label(getOptionalStr(!value.isManatory()));
				I18nLabel main = new I18nLabel(msg);
				main.setValue(value.getText());
				VerticalLayout vl = new VerticalLayout(mandatory, main);
				vl.setSpacing(false);
				vl.setMargin(false);
				return vl;
			}
		});
		agreements.setMargin(true);
		agreementsP = new SafePanel(msg.getMessage("RegistrationFormViewer.agreements"), agreements);
		
		attributeParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AttributeRegistrationParam>()
		{
			@Override
			public Label toLabel(AttributeRegistrationParam value)
			{
				String showGroup = value.isShowGroups() ? 
						"[" + msg.getMessage("RegistrationFormViewer.showAttributeGroup")+"]" : ""; 
				HtmlLabel ret = new HtmlLabel(msg);
				String line1 = value.getAttributeType() + " @ " + value.getGroup() + " " +
						showGroup;
				ret.setHtmlValue("RegistrationFormViewer.twoLines", line1, toHTMLLabel(value));
				return ret;
			}
		});
		attributeParams.setMargin(true);
		attributeParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.attributeParams"), 
				attributeParams);
		
		
		groupParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<GroupRegistrationParam>()
		{
			@Override
			public Label toLabel(GroupRegistrationParam value)
			{
				HtmlLabel ret = new HtmlLabel(msg);
				ret.setHtmlValue("RegistrationFormViewer.twoLines", value.getGroupPath(), 
						toHTMLLabel(value));
				return ret;
			}
		});
		groupParams.setMargin(true);
		groupParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.groupParams"), groupParams);
		
		wrapper.addComponents(localSignUpMethodsP, attributeParamsP, groupParamsP, agreementsP);
		return wrapper;
	}
	
	private String toHTMLLabel(OptionalRegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return emptyNull(value.getLabel()) + " " + getOptionalStr(value.isOptional()) + " [" + settings + "] [" + 
				emptyNull(value.getDescription()) + "]";
	}

	private String toHTMLLabel(RegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return emptyNull(value.getLabel()) + " [" + settings + "] [" + emptyNull(value.getDescription()) + "]";
	}
	
	private String getOptionalStr(boolean value)
	{
		return "[" + (value ? msg.getMessage("RegistrationFormViewer.optional") : 
			msg.getMessage("RegistrationFormViewer.mandatory")) + "]";
	}
	
	private String emptyNull(String a)
	{
		return a == null ? "" : a;
	}
}
