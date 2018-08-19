/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.DescribedObjectROImpl;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;


/**
 * Base class with shared state for {@link RegistrationForm} and {@link EnquiryForm}.
 *  
 *  
 * @author K. Benedyczak
 */
public abstract class BaseForm extends DescribedObjectROImpl
{
	private List<IdentityRegistrationParam> identityParams = new ArrayList<>();
	private List<AttributeRegistrationParam> attributeParams = new ArrayList<>();	
	private List<GroupRegistrationParam> groupParams = new ArrayList<>();
	private List<CredentialRegistrationParam> credentialParams = new ArrayList<>();
	private List<AgreementRegistrationParam> agreements = new ArrayList<>();
	private AuthenticationFlowsSpec authenticationFlows = new AuthenticationFlowsSpec();
	private boolean collectComments;
	private I18nString displayedName = new I18nString();
	private I18nString formInformation = new I18nString();
	private TranslationProfile translationProfile = 
			new TranslationProfile("registrationProfile", "", ProfileType.REGISTRATION, new ArrayList<>()); 
	private FormLayout layout;
	
	@JsonCreator
	BaseForm(ObjectNode json)
	{
		fromJson(json);
		validate();
	}
	
	BaseForm()
	{
	}

	protected final void validate()
	{
		if (identityParams == null || groupParams == null || agreements == null 
				|| credentialParams == null || attributeParams == null)
			throw new IllegalStateException("All lists must be not-null in a form");
		if (translationProfile == null)
			throw new IllegalStateException("Translation profile must be not-null "
					+ "in a form");
		if (name == null)
			throw new IllegalStateException("Name must be not-null "
					+ "in a form");
		if (displayedName == null)
			throw new IllegalStateException("Displayed name must be not-null "
					+ "in a form (but it contents can be empty)");
		if (formInformation == null)
			throw new IllegalStateException("Form information must be not-null "
					+ "in a form (but it contents can be empty)");
	}
	
	/**
	 * removes all elements in layout that are not present in form and adds all form elements missing in layout
	 * at the end of it.
	 */
	public void updateLayout()
	{
		if (layout == null)
			return;
		Set<String> definedElements = layout.getDefinedElements();
		updateFormParametersInLayout(definedElements);
		updateOtherElementsInLayout(definedElements);
	}

	protected void updateFormParametersInLayout(Set<String> definedElements)
	{
		for (int i = 0; i < identityParams.size(); i++)
			layout.addParameterIfMissing(FormLayout.IDENTITY, i, definedElements);
		for (int i = 0; i < attributeParams.size(); i++)
			layout.addParameterIfMissing(FormLayout.ATTRIBUTE, i, definedElements);
		for (int i = 0; i < agreements.size(); i++)
			layout.addParameterIfMissing(FormLayout.AGREEMENT, i, definedElements);
		for (int i = 0; i < groupParams.size(); i++)
			layout.addParameterIfMissing(FormLayout.GROUP, i, definedElements);
		for (int i = 0; i < credentialParams.size(); i++)
			layout.addParameterIfMissing(FormLayout.CREDENTIAL, i, definedElements);
		
		layout.removeParametersWithIndexLargerThen(FormLayout.IDENTITY, identityParams.size());
		layout.removeParametersWithIndexLargerThen(FormLayout.ATTRIBUTE, attributeParams.size());
		layout.removeParametersWithIndexLargerThen(FormLayout.AGREEMENT, agreements.size());
		layout.removeParametersWithIndexLargerThen(FormLayout.GROUP, groupParams.size());
		layout.removeParametersWithIndexLargerThen(FormLayout.CREDENTIAL, credentialParams.size());
	}
	
	protected void updateOtherElementsInLayout(Set<String> definedElements)
	{
		if (isCollectComments())
			layout.addBasicElementIfMissing(FormLayout.COMMENTS, definedElements);
		else
			layout.removeBasicElementIfPresent(FormLayout.COMMENTS);
	}

	
	protected void checkOtherElementsInLayout(Set<String> definedElements)
	{
		if (isCollectComments())
			layout.checkLayoutElement(FormLayout.COMMENTS, definedElements);
	}

	protected void checkFormParametersInLayout(Set<String> definedElements)
	{
		for (int i = 0; i < identityParams.size(); i++)
			layout.checkLayoutElement(FormLayout.IDENTITY + "_" + i, definedElements);
		for (int i = 0; i < attributeParams.size(); i++)
			layout.checkLayoutElement(FormLayout.ATTRIBUTE + "_" + i, definedElements);
		for (int i = 0; i < agreements.size(); i++)
			layout.checkLayoutElement(FormLayout.AGREEMENT + "_" + i, definedElements);
		for (int i = 0; i < groupParams.size(); i++)
			layout.checkLayoutElement(FormLayout.GROUP + "_" + i, definedElements);
		for (int i = 0; i < credentialParams.size(); i++)
			layout.checkLayoutElement(FormLayout.CREDENTIAL + "_" + i, definedElements);
	}
	
	protected void validateLayout()
	{
		Set<String> definedElements = layout.getDefinedElements();
		
		checkFormParametersInLayout(definedElements);
		checkOtherElementsInLayout(definedElements);
		if (!definedElements.isEmpty())
			throw new IllegalStateException("Form layout contains elements "
					+ "which are not defied in the form: " + definedElements);
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = jsonMapper.createObjectNode();
		root.set("Agreements", serializeAgreements(jsonMapper, getAgreements()));
		root.set("AttributeParams", jsonMapper.valueToTree(getAttributeParams()));
		root.put("CollectComments", isCollectComments());
		root.set("CredentialParams", jsonMapper.valueToTree(getCredentialParams()));
		root.put("Description", getDescription());
		root.set("i18nFormInformation", I18nStringJsonUtil.toJson(getFormInformation()));
		root.set("GroupParams", jsonMapper.valueToTree(getGroupParams()));
		root.set("IdentityParams", jsonMapper.valueToTree(getIdentityParams()));
		root.put("Name", getName());
		root.set("DisplayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		root.set("TranslationProfile", getTranslationProfile().toJsonObject());
		if (layout != null)
			root.set("FormLayout", jsonMapper.valueToTree(getLayout()));
		root.set("AuthenticationFlowsSpec", getAuthenticationFlows().toJsonObject());
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		try
		{
			JsonNode n = root.get("Agreements");
			if (n != null)
			{
				setAgreements(loadAgreements((ArrayNode) n));
			}
			
			n = root.get("AttributeParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AttributeRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<AttributeRegistrationParam>>(){});
				setAttributeParams(r);
			}
			n = root.get("CollectComments");
			setCollectComments(n.asBoolean());
			n = root.get("CredentialParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<CredentialRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<CredentialRegistrationParam>>(){});
				setCredentialParams(r);
			}
			n = root.get("Description");
			setDescription((n == null || n.isNull()) ? null : n.asText());
			
			setFormInformation(I18nStringJsonUtil.fromJson(root.get("i18nFormInformation"), 
					root.get("FormInformation")));
			
			n = root.get("GroupParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<GroupRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<GroupRegistrationParam>>(){});
				setGroupParams(r);
			}

			n = root.get("IdentityParams");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<IdentityRegistrationParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<IdentityRegistrationParam>>(){});
				setIdentityParams(r);
			}

			n = root.get("Name");
			setName(n.asText());
			
			if (root.has("DisplayedName"))
				setDisplayedName(I18nStringJsonUtil.fromJson(root.get("DisplayedName")));
			
			n = root.get("TranslationProfile");
			if (n != null)
			{
				setTranslationProfile(new TranslationProfile((ObjectNode) n));
			}

			n = root.get("FormLayout");
			if (n != null)
			{
				setLayout(jsonMapper.treeToValue(n, FormLayout.class));
			}
			
			n = root.get("AuthenticationFlowsSpec");
			if (n != null)
			{
				setAuthenticationFlows(new AuthenticationFlowsSpec((ObjectNode) n));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize a form from JSON", e);
		}
	}

	private JsonNode serializeAgreements(ObjectMapper jsonMapper, List<AgreementRegistrationParam> agreements)
	{
		ArrayNode root = jsonMapper.createArrayNode();
		for (AgreementRegistrationParam agreement: agreements)
		{
			ObjectNode node = root.addObject();
			node.set("i18nText", I18nStringJsonUtil.toJson(agreement.getText()));
			node.put("manatory", agreement.isManatory());
		}
		return root;
	}

	private List<AgreementRegistrationParam> loadAgreements(ArrayNode root)
	{
		List<AgreementRegistrationParam> ret = new ArrayList<>();
		
		for (JsonNode nodeR: root)
		{
			ObjectNode node = (ObjectNode) nodeR;
			AgreementRegistrationParam param = new AgreementRegistrationParam();
			ret.add(param);
			
			param.setText(I18nStringJsonUtil.fromJson(node.get("i18nText"), node.get("text")));
			param.setManatory(node.get("manatory").asBoolean());
		}
		
		return ret;
	}

	public boolean containsAutomaticAndMandatoryParams()
	{
		if (identityParams != null)
		{
			for (IdentityRegistrationParam id : identityParams)
			{
				if (checkAutoParam(id))
					return true;
			}
		}
		if (attributeParams != null)
		{
			for (AttributeRegistrationParam at : attributeParams)
			{
				if (checkAutoParam(at))
					return true;
			}
		}
		if (groupParams != null)
		{
			for (GroupRegistrationParam gr : groupParams)
			{
				if (gr.getRetrievalSettings().isAutomaticOnly())
					return true;
			}
		}
		return false;
	}
	
	private boolean checkAutoParam(OptionalRegistrationParam param)
	{
		if (!param.isOptional() && (param.getRetrievalSettings().isAutomaticOnly()))
			return true;

		return false;
	}
	
	void setName(String name)
	{
		this.name = name;
		if (displayedName == null)
			displayedName = new I18nString(name);
	}
	
	void setDescription(String description)
	{
		this.description = description;
	}
	
	public List<IdentityRegistrationParam> getIdentityParams()
	{
		return identityParams;
	}

	void setIdentityParams(List<IdentityRegistrationParam> identityParams)
	{
		this.identityParams = identityParams;
	}

	public List<AttributeRegistrationParam> getAttributeParams()
	{
		return attributeParams;
	}

	void setAttributeParams(List<AttributeRegistrationParam> attributeParams)
	{
		this.attributeParams = attributeParams;
	}

	public List<GroupRegistrationParam> getGroupParams()
	{
		return groupParams;
	}

	void setGroupParams(List<GroupRegistrationParam> groupParams)
	{
		this.groupParams = groupParams;
	}

	public List<CredentialRegistrationParam> getCredentialParams()
	{
		return credentialParams;
	}

	void setCredentialParams(List<CredentialRegistrationParam> credentialParams)
	{
		this.credentialParams = credentialParams;
	}

	public List<AgreementRegistrationParam> getAgreements()
	{
		return agreements;
	}

	void setAgreements(List<AgreementRegistrationParam> agreements)
	{
		this.agreements = agreements;
	}

	public boolean isCollectComments()
	{
		return collectComments;
	}

	void setCollectComments(boolean collectComments)
	{
		this.collectComments = collectComments;
	}
	public I18nString getFormInformation()
	{
		return formInformation;
	}

	void setFormInformation(I18nString formInformation)
	{
		this.formInformation = formInformation;
	}

	public I18nString getDisplayedName()
	{
		return displayedName == null ? new I18nString(getName()) : displayedName;
	}

	void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}
	
	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}
	
	public AuthenticationFlowsSpec getAuthenticationFlows()
	{
		return authenticationFlows;
	}

	public void setAuthenticationFlows(AuthenticationFlowsSpec authenticationFlows)
	{
		this.authenticationFlows = authenticationFlows;
	}

	void setTranslationProfile(TranslationProfile translationProfile)
	{
		if (translationProfile.getProfileType() != ProfileType.REGISTRATION)
			throw new IllegalArgumentException("Only a registration profile can be used with registration form");
		this.translationProfile = translationProfile;
	}

	public FormLayout getEffectiveFormLayout(MessageSource msg)
	{
		return layout == null ? getDefaultFormLayout(msg) : layout;
	}
	
	public FormLayout getLayout()
	{
		return layout;
	}
	
	public abstract FormLayout getDefaultFormLayout(MessageSource msg);
	
	public void setLayout(FormLayout layout)
	{
		this.layout = layout;
	}

	protected List<FormElement> getDefaultBasicParamsLayout(String type, List<?> params, 
			MessageSource msg, String captionKey, boolean addSeparator)
	{
		List<FormElement> ret = new ArrayList<>();
		if (!params.isEmpty())
			ret.add(new FormCaptionElement(new I18nString(captionKey, msg)));
		for (int i=0; i<params.size(); i++)
		{
			if (addSeparator && i > 0)
				ret.add(new FormSeparatorElement());
			ret.add(new FormParameterElement(type, i));
		}
		return ret;
	}
	
	protected List<FormElement> getDefaultParametersLayout(String type, List<? extends RegistrationParam> params, 
			MessageSource msg, String captionKey, String readOnlyCaptionKey)
	{
		List<FormElement> ret = new ArrayList<>();
		
		for (int i=0; i<params.size(); i++)
		{
			RegistrationParam param = params.get(i);
			if (param.getRetrievalSettings().isInteractivelyEntered(false))
				ret.add(new FormParameterElement(type, i));
		}
		
		if (!ret.isEmpty())
			ret.add(0, new FormCaptionElement(new I18nString(captionKey, msg)));

		int interactiveSize = ret.size();
		for (int i=0; i<params.size(); i++)
		{
			RegistrationParam param = params.get(i);
			if (param.getRetrievalSettings().isPotentiallyAutomaticAndVisible())
				ret.add(new FormParameterElement(type, i));
		}
		
		if (interactiveSize < ret.size())
			ret.add(interactiveSize, new FormCaptionElement(new I18nString(readOnlyCaptionKey, msg)));
		
		return ret;
	}
	
	public abstract BaseFormNotifications getNotificationsConfiguration();

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((agreements == null) ? 0 : agreements.hashCode());
		result = prime * result + ((attributeParams == null) ? 0 : attributeParams.hashCode());
		result = prime * result + ((authenticationFlows == null) ? 0 : authenticationFlows.hashCode());
		result = prime * result + (collectComments ? 1231 : 1237);
		result = prime * result + ((credentialParams == null) ? 0 : credentialParams.hashCode());
		result = prime * result + ((displayedName == null) ? 0 : displayedName.hashCode());
		result = prime * result + ((formInformation == null) ? 0 : formInformation.hashCode());
		result = prime * result + ((groupParams == null) ? 0 : groupParams.hashCode());
		result = prime * result + ((identityParams == null) ? 0 : identityParams.hashCode());
		result = prime * result + ((layout == null) ? 0 : layout.hashCode());
		result = prime * result + ((translationProfile == null) ? 0 : translationProfile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseForm other = (BaseForm) obj;
		if (agreements == null)
		{
			if (other.agreements != null)
				return false;
		} else if (!agreements.equals(other.agreements))
			return false;
		if (attributeParams == null)
		{
			if (other.attributeParams != null)
				return false;
		} else if (!attributeParams.equals(other.attributeParams))
			return false;
		if (authenticationFlows == null)
		{
			if (other.authenticationFlows != null)
				return false;
		} else if (!authenticationFlows.equals(other.authenticationFlows))
			return false;
		if (collectComments != other.collectComments)
			return false;
		if (credentialParams == null)
		{
			if (other.credentialParams != null)
				return false;
		} else if (!credentialParams.equals(other.credentialParams))
			return false;
		if (displayedName == null)
		{
			if (other.displayedName != null)
				return false;
		} else if (!displayedName.equals(other.displayedName))
			return false;
		if (formInformation == null)
		{
			if (other.formInformation != null)
				return false;
		} else if (!formInformation.equals(other.formInformation))
			return false;
		if (groupParams == null)
		{
			if (other.groupParams != null)
				return false;
		} else if (!groupParams.equals(other.groupParams))
			return false;
		if (identityParams == null)
		{
			if (other.identityParams != null)
				return false;
		} else if (!identityParams.equals(other.identityParams))
			return false;
		if (layout == null)
		{
			if (other.layout != null)
				return false;
		} else if (!layout.equals(other.layout))
			return false;
		if (translationProfile == null)
		{
			if (other.translationProfile != null)
				return false;
		} else if (!translationProfile.equals(other.translationProfile))
			return false;
		return true;
	}
}
