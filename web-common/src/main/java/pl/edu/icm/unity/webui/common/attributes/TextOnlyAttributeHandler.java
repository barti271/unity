/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.List;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.ext.AttributeHandlerHelper;

/**
 * Base attribute handler for the web. Renders as label, edit in text field. Extensions has to implement
 * only id and conversion from String to domain type.
 * @author K. Benedyczak
 */
public abstract class TextOnlyAttributeHandler implements WebAttributeHandler
{
	protected AttributeValueSyntax<?> syntax;
	protected UnityMessageSource msg;
	
	public static final int LARGE_STRING = 1000;
	
	public TextOnlyAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		this.syntax = syntax;
		this.msg = msg;
	}

	@Override
	public String getValueAsString(String value)
	{
		return value.toString();
	}
	
	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		return AttributeHandlerHelper.getRepresentation(value, syntax, context);
	}
	
	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new StringValueEditor(initialValue, label, syntax);
	}
	
	private class StringValueEditor implements AttributeValueEditor
	{
		private String value;
		private String label;
		private AttributeValueSyntax<?> syntax;
		private AbstractTextField field;
		private boolean required;
		private AttributeEditContext context;
		
		public StringValueEditor(String value, String label, AttributeValueSyntax<?> syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			this.required = context.isRequired();
			this.context = context;
			boolean large = false;
			if (syntax instanceof StringAttributeSyntax)
			{
				StringAttributeSyntax sas = (StringAttributeSyntax) syntax;
				if (sas.getMaxLength() > LARGE_STRING)
					large = true;
				this.required = required && sas.getMinLength() > 0;
			}
			
			field = large ? new TextArea() : new TextField();
			if (large)
				field.setWidth(60, Unit.PERCENTAGE);
			if (value != null)
				field.setValue(value.toString());
			field.setRequiredIndicatorVisible(this.required);
			setLabel(label);
			
			StringBuilder sb = new StringBuilder();
			for (String hint: getHints())
				sb.append(hint).append("<br>");
			field.setDescription(sb.toString(), ContentMode.HTML);
			if (label != null)
				field.setId("ValueEditor."+label);
			
			if (context.isCustomWidth())
				field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			
			return new ComponentsContainer(field);
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			
			if (!required && field.getValue().isEmpty())
				return field.getValue();
			
			try
			{
				String cur= field.getValue();
				syntax.validateStringValue(cur);
				field.setComponentError(null);
				return cur;
			} catch (IllegalAttributeValueException e)
			{
				field.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				field.setComponentError(new UserError(e.getMessage()));
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}
		}

		@Override
		public void setLabel(String label)
		{
			if (context.isShowLabelInline())
				field.setPlaceholder(label);
			else
				field.setCaption(label);
		}
	}

	protected abstract List<String> getHints();
	
	@Override
	public Component getSyntaxViewer()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(false);
		ret.setMargin(false);
		for (String hint: getHints())
		{
			Label info = new Label(hint);
			ret.addComponent(info);
		}
		return ret;
	}
}
