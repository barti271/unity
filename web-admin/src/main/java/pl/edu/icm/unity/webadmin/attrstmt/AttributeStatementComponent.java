/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;

import com.google.common.collect.ImmutableMap;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attribute.AttributeFieldWithEdit;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox2;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox2;

/**
 * Editing of a single {@link AttributeStatement}.
 * @author K. Benedyczak
 */
public class AttributeStatementComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeStatementComponent.class);
	private static final String MODE_FIXED = "fixed";
	private static final String MODE_DYNAMIC = "dynamic";
	
	private final UnityMessageSource msg;
	private final Set<String> groups;
	private final Collection<AttributeType> attributeTypes;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final String group;
	
	private CheckBox extraAttributesGroupCB;
	private GroupComboBox2 extraAttributesGroupCombo;
	private TextField condition;
	private RadioButtonGroup<String> assignMode;
	private ComboBox<AttributeType> dynamicAttributeName;
	private TextField dynamicAttributeValue;
	private AttributeFieldWithEdit fixedAttribute;
	private ComboBox<AttributeStatement.ConflictResolution> conflictResolution;

	private Binder<AttributeStatement> binder;
	
	
	public AttributeStatementComponent(UnityMessageSource msg, GroupsManagement groupsMan,
			Collection<AttributeType> attributeTypes,
			AttributeHandlerRegistry attrHandlerRegistry, String group)
	{
		this.msg = msg;
		this.groups = getGroupsOfHierarchy(groupsMan, group);
		this.attributeTypes = attributeTypes;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.group = group;
		initUI();
	}

	private Set<String> getGroupsOfHierarchy(GroupsManagement groupsMan,
			String hierarchyElement)
	{
		try
		{
			Set<String> allGroups = groupsMan.getChildGroups("/");
			return allGroups.stream().filter(g -> !g.equals(hierarchyElement)
					&& (g.startsWith(hierarchyElement)
							|| hierarchyElement.startsWith(g)))
					.collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.warn("Can not read child groups", e);
			return new HashSet<>();
		}
	}

	private void initUI()
	{
		extraAttributesGroupCB = new CheckBox(
				msg.getMessage("AttributeStatementComponent.extraGroupCB"));
		extraAttributesGroupCB.addValueChangeListener((e) -> {
			extraAttributesGroupCombo.setEnabled(extraAttributesGroupCB.getValue());
			extraAttributesGroupCombo.setRequiredIndicatorVisible(extraAttributesGroupCB.getValue());
		});
		extraAttributesGroupCB.setDescription(
				msg.getMessage("AttributeStatementComponent.extraGroupCBDesc"));

		extraAttributesGroupCombo = new GroupComboBox2(
				msg.getMessage("AttributeStatementComponent.extraGroupSelect"),
				groups);
		extraAttributesGroupCombo.setEnabled(false);
		extraAttributesGroupCombo.setInput(group, false);
		extraAttributesGroupCombo.setEmptySelectionAllowed(false);
		extraAttributesGroupCombo.setRequiredIndicatorVisible(false);
		
		if (groups.isEmpty())
			extraAttributesGroupCB.setEnabled(false);

		condition = new TextField(msg.getMessage("AttributeStatementComponent.condition"));
		condition.setDescription(
				msg.getMessage("AttributeStatementComponent.conditionDesc"));
		condition.setValue("true");

		assignMode = new RadioButtonGroup<>();
		Map<String, String> captions = ImmutableMap.of(MODE_DYNAMIC,
				msg.getMessage("AttributeStatementComponent.dynamicMode"),
				MODE_FIXED,
				msg.getMessage("AttributeStatementComponent.fixedMode"));
		assignMode.setItems(captions.keySet());
		assignMode.setItemCaptionGenerator(i -> captions.get(i));
		assignMode.addValueChangeListener((e) -> {
			boolean fixed = assignMode.getValue().equals(MODE_FIXED);
			fixedAttribute.setVisible(fixed);
			dynamicAttributeName.setVisible(!fixed);
			dynamicAttributeValue.setVisible(!fixed);
		});
		
		dynamicAttributeName =  new AttributeSelectionComboBox2(
				msg.getMessage("AttributeStatementComponent.dynamicAttrName"), attributeTypes);
		
		dynamicAttributeValue = new TextField(
				msg.getMessage("AttributeStatementComponent.dynamicAttrValue"));
		dynamicAttributeValue.setDescription(
				msg.getMessage("AttributeStatementComponent.dynamicAttrValueDesc"));

		fixedAttribute = new AttributeFieldWithEdit(msg,
				msg.getMessage("AttributeStatementComponent.fixedAttr"),
				attrHandlerRegistry, attributeTypes, group, null, true);

		assignMode.setValue(MODE_DYNAMIC);

		conflictResolution = new ComboBox<AttributeStatement.ConflictResolution>(
				msg.getMessage("AttributeStatementEditDialog.conflictResolution"));
		conflictResolution.setItems(AttributeStatement.ConflictResolution.values());
		conflictResolution.setItemCaptionGenerator(item -> msg.getMessage(
				"AttributeStatement.conflictResolution." + item.toString()));
		conflictResolution.setValue(AttributeStatement.ConflictResolution.skip);
		conflictResolution.setEmptySelectionAllowed(false);

		FormLayout main = new CompactFormLayout();
		setCompositionRoot(main);
		main.addComponents(extraAttributesGroupCB, extraAttributesGroupCombo, condition,
				assignMode, dynamicAttributeName, dynamicAttributeValue,
				fixedAttribute, conflictResolution);

		Validator<String> expressionValidator = getExpressionValidator();
		binder = new Binder<>(AttributeStatement.class);

		binder.forField(extraAttributesGroupCombo)
				.asRequired(msg.getMessage("fieldRequired"))
				.withNullRepresentation(extraAttributesGroupCombo.getValue())
				.bind("extraAttributesGroup");
		binder.forField(condition).withValidator(expressionValidator)
				.asRequired(msg.getMessage("fieldRequired")).bind("condition");
		binder.forField(fixedAttribute).asRequired(msg.getMessage("fieldRequired"))
				.bind("fixedAttribute");
		Map<String, AttributeType> typesMap = attributeTypes.stream()
				.collect(Collectors.toMap(t -> t.getName(), t -> t));
		binder.forField(dynamicAttributeName)
				.withConverter(d -> d.getName(), d -> typesMap.get(d))
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("dynamicAttributeType");
		binder.forField(dynamicAttributeValue).withValidator(expressionValidator)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("dynamicAttributeExpression");
		binder.forField(conflictResolution).asRequired(msg.getMessage("fieldRequired"))
				.bind("conflictResolution");

		AttributeStatement attrStatment = new AttributeStatement();
		attrStatment.setCondition(condition.getValue());
		attrStatment.setConflictResolution(ConflictResolution.skip);
		attrStatment.setExtraAttributesGroup(extraAttributesGroupCombo.getValue());
		attrStatment.setDynamicAttributeType(dynamicAttributeName.getValue().getName());
		binder.setBean(attrStatment);

	}

	private Validator<String> getExpressionValidator()
	{
		Validator<String> expressionValidator = new Validator<String>()
		{

			@Override
			public ValidationResult apply(String value, ValueContext context)
			{
				try
				{
					MVEL.compileExpression(value);
				} catch (Exception e)
				{
					return ValidationResult.error(msg.getMessage(
							"AttributeStatementComponent.invalidExpression")
							+ ":" + e.getMessage());

				}
				return ValidationResult.ok();
			}
		};
		return expressionValidator;

	}
	
	public void setInitialData(AttributeStatement initial)
	{
		binder.setBean(initial);
		extraAttributesGroupCB.setValue(initial.getExtraAttributesGroup() != null);
		assignMode.setValue(initial.dynamicAttributeMode() ? MODE_DYNAMIC : MODE_FIXED);
	}

	private void validateBinding(String name) throws FormValidationException
	{
		if (binder.getBinding(name).get().validate().isError())
			throw new FormValidationException();
	}
	
	public AttributeStatement getStatementFromComponent() throws FormValidationException
	{
		AttributeStatement ret = binder.getBean();

		validateBinding("condition");
		validateBinding("conflictResolution");
		
		if (!extraAttributesGroupCB.getValue())
		{
			ret.setExtraAttributesGroup(null);
		} else
		{
			validateBinding("extraAttributesGroup");
		}

		if (assignMode.getValue().equals(MODE_DYNAMIC))
		{
			validateBinding("dynamicAttributeType");
			validateBinding("dynamicAttributeExpression");	
			ret.setFixedAttribute(null);
		} else
		{
			validateBinding("fixedAttribute");
			ret.setDynamicAttributeExpression(null);
			ret.setDynamicAttributeType(null);
		}

		return ret;
	}
}
