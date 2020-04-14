/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import java.util.function.Consumer;

import com.vaadin.ui.Component;

import io.imunity.webadmin.bulk.RuleEditor;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Editor dialog for rule editing.
 * @author K. Benedyczak
 */
public class RuleEditDialog<T extends TranslationRule> extends AbstractDialog
{
	private Consumer<T> callback;
	private RuleEditor<T> editor;

	public RuleEditDialog(MessageSource msg, String caption, RuleEditor<T> editor,
			Consumer<T> callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			callback.accept(editor.getRule());
		} catch (FormValidationException e)
		{
			NotificationPopup.showFormError(msg);
			return;
		}
		close();
	}
}
