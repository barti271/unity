/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import com.vaadin.ui.Component;

import io.imunity.webadmin.attributeclass.AttributesClassEditor;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Dialog allowing to edit {@link AttributesClass}. It takes an editor component 
 * {@link AttributesClassEditor} as argument.
 * @author K. Benedyczak
 */
public class AttributesClassEditDialog extends AbstractDialog
{
	private AttributesClassEditor editor;
	private Callback callback;
	
	public AttributesClassEditDialog(MessageSource msg, String caption, AttributesClassEditor editor, 
			Callback callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents()
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			AttributesClass attributesClass = editor.getAttributesClass();
			if (callback.newAttributesClass(attributesClass))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showFormError(msg);
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newAttributesClass(AttributesClass newAC);
	}

}
