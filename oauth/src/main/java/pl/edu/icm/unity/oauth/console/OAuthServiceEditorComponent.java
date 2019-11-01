/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.console;

import static pl.edu.icm.unity.oauth.console.OAuthServiceController.IDP_CLIENT_MAIN_GROUP;
import static pl.edu.icm.unity.oauth.console.OAuthServiceController.OAUTH_CLIENTS_SUBGROUP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.oauth.console.OAuthClient.OAuthClientsBean;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.console.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.console.services.idp.IdpUser;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

class OAuthServiceEditorComponent extends ServiceEditorBase
{
	public static final String TOKEN_SERVICE_NAME_SUFFIX = "_TOKEN";

	private Binder<DefaultServiceDefinition> oauthServiceWebAuthzBinder;
	private Binder<DefaultServiceDefinition> oauthServiceTokenBinder;
	private Binder<OAuthServiceConfiguration> oauthConfigBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private Binder<OAuthClientsBean> clientsBinder;
	private FileStorageService fileStorageService;
	private Group generatedIdPGroup;
	private boolean editMode;

	OAuthServiceEditorComponent(UnityMessageSource msg, 
			OAuthEditorGeneralTab generalTab,
			OAuthEditorClientsTab clientsTab,
			URIAccessService uriAccessService, 
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, 
			ServiceDefinition toEdit, 
			List<String> allRealms, 
			List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, 
			List<Group> allGroups, 
			List<IdpUser> allUsers,
			Function<String, List<OAuthClient>> systemClientsSupplier, 
			List<String> registrationForms, 
			AuthenticatorSupportService authenticatorSupportService, 
			List<String> attrTypes)
	{
		super(msg);
		editMode = toEdit != null;

		this.fileStorageService = fileStorageService;

		oauthServiceWebAuthzBinder = new Binder<>(DefaultServiceDefinition.class);
		oauthConfigBinder = new Binder<>(OAuthServiceConfiguration.class);
		oauthServiceTokenBinder = new Binder<>(DefaultServiceDefinition.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		clientsBinder = new Binder<>(OAuthClientsBean.class);

		List<Group> groupsWithAutoGen = new ArrayList<>();
		groupsWithAutoGen.addAll(allGroups);
		generatedIdPGroup = generateRandomIdPGroup(allGroups);
		Group generatedClientsGroup = new Group(generatedIdPGroup, OAUTH_CLIENTS_SUBGROUP);
		generatedClientsGroup.setDisplayedName(new I18nString(OAUTH_CLIENTS_SUBGROUP));

		if (!editMode)
		{
			if (allGroups.stream().map(g -> g.toString())
					.filter(g -> g.equals(IDP_CLIENT_MAIN_GROUP)).count() == 0)
			{
				groupsWithAutoGen.add(new Group(IDP_CLIENT_MAIN_GROUP));
			}

			groupsWithAutoGen.add(generatedIdPGroup);
			groupsWithAutoGen.add(generatedClientsGroup);
		}

		generalTab.initUI(oauthServiceWebAuthzBinder, oauthServiceTokenBinder, oauthConfigBinder);
		clientsTab.initUI(groupsWithAutoGen, oauthServiceTokenBinder, oauthConfigBinder, clientsBinder);

		IdpEditorUsersTab usersTab = new IdpEditorUsersTab(msg, oauthConfigBinder, allGroups, allUsers,
				attrTypes);

		generalTab.addNameValueChangeListener(e -> 
		{
			String displayedName = (e.getValue() != null && !e.getValue().isEmpty()) ?
					e.getValue() : generatedIdPGroup.toString();
			generatedIdPGroup.setDisplayedName(new I18nString(displayedName));
			clientsTab.refreshGroups();
		});

		registerTab(generalTab);
		registerTab(clientsTab);
		registerTab(usersTab);
		registerTab(new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding(), oauthServiceWebAuthzBinder,
				webConfigBinder, msg.getMessage("IdpServiceEditorBase.authentication")));

		OAuthServiceDefinition oauthServiceToEdit;
		OAuthServiceConfiguration oauthConfig = new OAuthServiceConfiguration(allGroups);
		oauthConfig.setClientGroup(new GroupWithIndentIndicator(generatedClientsGroup, false));

		DefaultServiceDefinition webAuthzService = new DefaultServiceDefinition(
				OAuthAuthzWebEndpoint.Factory.TYPE.getName());
		DefaultServiceDefinition tokenService = new DefaultServiceDefinition(OAuthTokenEndpoint.TYPE.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		OAuthClientsBean clientsBean = new OAuthClientsBean();

		if (editMode)
		{
			oauthServiceToEdit = (OAuthServiceDefinition) toEdit;
			webAuthzService = oauthServiceToEdit.getWebAuthzService();
			tokenService = oauthServiceToEdit.getTokenService();

			if (webAuthzService != null && webAuthzService.getConfiguration() != null)
			{
				oauthConfig.fromProperties(webAuthzService.getConfiguration(), msg, allGroups);
				webConfig.fromProperties(webAuthzService.getConfiguration(), msg, uriAccessService);
			}
			clientsBean.setClients(cloneClients(systemClientsSupplier.apply(
					oauthConfig.getClientGroup().group.toString())));
		}

		oauthConfigBinder.setBean(oauthConfig);
		clientsBinder.setBean(clientsBean);
		oauthServiceWebAuthzBinder.setBean(webAuthzService);
		oauthServiceTokenBinder.setBean(tokenService);
		webConfigBinder.setBean(webConfig);

		if (editMode)
		{
			oauthServiceWebAuthzBinder.validate();
			oauthServiceTokenBinder.validate();
		}

		Runnable refreshClients = () -> usersTab.setAvailableClients(
				clientsTab.getActiveClients().stream().collect(
					Collectors.toMap(c -> c.getId(), 
							c -> c.getName() == null || c.getName().isEmpty() ? 
									c.getId() : c.getName())));
		clientsBinder.addValueChangeListener(e -> refreshClients.run());
		clientsTab.addGroupValueChangeListener(e -> 
		{
			Group newGroup = e.getValue().group;
			List<OAuthClient> newGroupClients = 
					(newGroup.equals(generatedClientsGroup) || newGroup.equals(generatedIdPGroup)) ?
					Collections.emptyList() : cloneClients(systemClientsSupplier.apply(newGroup.toString()));
			clientsBean.setClients(newGroupClients);
			clientsBinder.setBean(clientsBean);
			refreshClients.run();
		});
		refreshClients.run();
	}

	private Group generateRandomIdPGroup(List<Group> allGroups)
	{
		String genPath = null;
		do
		{
			genPath = OAuthServiceController.IDP_CLIENT_MAIN_GROUP + "/" + 
					RandomStringUtils.randomAlphabetic(6).toLowerCase();
		} while (checkIfGroupExists(allGroups, genPath));

		return new Group(genPath);
	}
	
	private boolean checkIfGroupExists(List<Group> allGroups, String path)
	{
		return allGroups.stream()
				.filter(group -> group.toString().equals(path))
				.findAny().isPresent();
	}

	private List<OAuthClient> cloneClients(List<OAuthClient> clients)
	{
		return clients.stream().map(OAuthClient::clone).collect(Collectors.toList()); 
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = oauthServiceWebAuthzBinder.validate().hasErrors();
		hasErrors |= oauthConfigBinder.validate().hasErrors();
		hasErrors |= oauthServiceTokenBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();

		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition webAuthz = oauthServiceWebAuthzBinder.getBean();
		VaadinEndpointProperties prop = new VaadinEndpointProperties(
				webConfigBinder.getBean().toProperties(msg, fileStorageService, webAuthz.getName()));
		webAuthz.setConfiguration(oauthConfigBinder.getBean().toProperties() + "\n" + prop.getAsString());

		DefaultServiceDefinition token = oauthServiceTokenBinder.getBean();
		token.setConfiguration(oauthConfigBinder.getBean().toProperties());

		if (token.getName() == null || token.getName().isEmpty())
		{
			token.setName(webAuthz.getName() + TOKEN_SERVICE_NAME_SUFFIX);
		}
		OAuthServiceDefinition def = new OAuthServiceDefinition(webAuthz, token);
		def.setSelectedClients(clientsBinder.getBean().getClients());
		if (!editMode)
		{
			def.setAutoGeneratedClientsGroup(generatedIdPGroup.toString());
		}
		return def;
	}

}