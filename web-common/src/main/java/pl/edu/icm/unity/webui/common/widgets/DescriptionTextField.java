/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.widgets;

import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;

public class DescriptionTextField extends TextField
{
	public DescriptionTextField(UnityMessageSource msg)
	{
		this();
		setCaption(msg.getMessage("ServiceEditorBase.description"));
	}
	
	public DescriptionTextField()
	{
		setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
	}
}
