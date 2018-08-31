/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Base of enquiry and registration requests.
 * 
 * @author K. Benedyczak
 */
public class BaseRegistrationInput
{
	private String formId;
	private List<IdentityParam> identities = new ArrayList<>();
	private List<Attribute> attributes = new ArrayList<>();
	private List<CredentialParamValue> credentials = new ArrayList<>();
	private List<GroupSelection> groupSelections = new ArrayList<>();
	private List<Selection> agreements = new ArrayList<>();
	private String comments;
	private String userLocale;
	
	public BaseRegistrationInput()
	{
	}

	@JsonCreator
	public BaseRegistrationInput(ObjectNode root)
	{
		try
		{
			fromJson(root);
		} catch (IOException e)
		{
			throw new IllegalArgumentException("Provided JSON is invalid", e);
		}
	}
	
	public void validate()
	{
		if (formId == null)
			throw new IllegalStateException("Form id must be set");
	}
	
	public String getFormId()
	{
		return formId;
	}

	public void setFormId(String formId)
	{
		this.formId = formId;
	}
	public List<IdentityParam> getIdentities()
	{
		return identities;
	}
	public void setIdentities(List<IdentityParam> identities)
	{
		this.identities = identities;
	}

	public List<Attribute> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes)
	{
		this.attributes = attributes;
	}

	public List<CredentialParamValue> getCredentials()
	{
		return credentials;
	}

	public void setCredentials(List<CredentialParamValue> credentials)
	{
		this.credentials = credentials;
	}

	public List<GroupSelection> getGroupSelections()
	{
		return groupSelections;
	}

	public void setGroupSelections(List<GroupSelection> groupSelections)
	{
		this.groupSelections = groupSelections;
	}

	public void addGroupSelection(GroupSelection groupSelection)
	{
		this.groupSelections.add(groupSelection);
	}

	public List<Selection> getAgreements()
	{
		return agreements;
	}

	public void setAgreements(List<Selection> agreements)
	{
		this.agreements = agreements;
	}

	public String getComments()
	{
		return comments;
	}

	public void setComments(String comments)
	{
		this.comments = comments;
	}

	public String getUserLocale()
	{
		return userLocale;
	}

	public void setUserLocale(String userLocale)
	{
		this.userLocale = userLocale;
	}

	@Override
	public String toString()
	{
		return "BaseRegistrationInput [formId=" + formId + ", identities=" + identities
				+ "]";
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = jsonMapper.createObjectNode();
		root.set("Agreements", jsonMapper.valueToTree(getAgreements()));
		root.set("Attributes", jsonMapper.valueToTree(getAttributes()));
		root.set("Comments", jsonMapper.valueToTree(getComments()));
		root.set("Credentials", jsonMapper.valueToTree(getCredentials()));
		root.set("FormId", jsonMapper.valueToTree(getFormId()));
		root.set("GroupSelections", jsonMapper.valueToTree(getGroupSelections()));
		root.set("Identities", jsonMapper.valueToTree(getIdentities()));
		root.put("UserLocale", getUserLocale());
		return root;
	}

	private void fromJson(ObjectNode root) throws IOException
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		JsonNode n = root.get("Agreements");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<Selection> r = jsonMapper.readValue(v, 
					new TypeReference<List<Selection>>(){});
			setAgreements(r);
		}
		
		n = root.get("Attributes");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<Attribute> r = jsonMapper.readValue(v, 
					new TypeReference<List<Attribute>>(){});
			setAttributes(r);	
		}
		

		n = root.get("Comments");
		if (n != null && !n.isNull())
			setComments(n.asText());
		
		n = root.get("Credentials");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<CredentialParamValue> r = jsonMapper.readValue(v, 
					new TypeReference<List<CredentialParamValue>>(){});
			setCredentials(r);
		}
		
		n = root.get("FormId");
		setFormId(n.asText());

		n = root.get("GroupSelections");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<GroupSelection> r = jsonMapper.readValue(v, 
					new TypeReference<List<GroupSelection>>(){});
			setGroupSelections(r);
		}

		n = root.get("Identities");			
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<IdentityParam> r = jsonMapper.readValue(v, 
					new TypeReference<List<IdentityParam>>(){});
			setIdentities(r);
		}
		
		n = root.get("UserLocale");
		if (n != null && !n.isNull())
			setUserLocale(n.asText());
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agreements == null) ? 0 : agreements.hashCode());
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((comments == null) ? 0 : comments.hashCode());
		result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
		result = prime * result + ((formId == null) ? 0 : formId.hashCode());
		result = prime * result
				+ ((groupSelections == null) ? 0 : groupSelections.hashCode());
		result = prime * result + ((identities == null) ? 0 : identities.hashCode());
		result = prime * result + ((userLocale == null) ? 0 : userLocale.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseRegistrationInput other = (BaseRegistrationInput) obj;
		if (agreements == null)
		{
			if (other.agreements != null)
				return false;
		} else if (!agreements.equals(other.agreements))
			return false;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (comments == null)
		{
			if (other.comments != null)
				return false;
		} else if (!comments.equals(other.comments))
			return false;
		if (credentials == null)
		{
			if (other.credentials != null)
				return false;
		} else if (!credentials.equals(other.credentials))
			return false;
		if (formId == null)
		{
			if (other.formId != null)
				return false;
		} else if (!formId.equals(other.formId))
			return false;
		if (groupSelections == null)
		{
			if (other.groupSelections != null)
				return false;
		} else if (!groupSelections.equals(other.groupSelections))
			return false;
		if (identities == null)
		{
			if (other.identities != null)
				return false;
		} else if (!identities.equals(other.identities))
			return false;
		if (userLocale == null)
		{
			if (other.userLocale != null)
				return false;
		} else if (!userLocale.equals(other.userLocale))
			return false;
		return true;
	}
}
