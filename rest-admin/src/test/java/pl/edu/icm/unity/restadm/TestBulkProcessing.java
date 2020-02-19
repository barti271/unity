/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import pl.edu.icm.unity.engine.bulkops.action.RemoveEntityActionFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Invitations management test
 * @author Krzysztof Benedyczak
 */
public class TestBulkProcessing extends RESTAdminTestBase
{
	@Test
	public void submittedProcessingActionIsExecuted() throws Exception
	{
		IdentityParam identityParam = new IdentityParam(UsernameIdentity.ID, "user-to-remove");	
		idsMan.addEntity(identityParam, CRED_REQ_PASS, EntityState.valid);
		
		HttpPost post = new HttpPost("/restadm/v1/bulkProcessing/instant?timeout=20");
		TranslationRule param = new TranslationRule(
				"(idsByType contains 'userName') && (idsByType['userName'] contains 'user-to-remove')", 
				new TranslationAction(RemoveEntityActionFactory.NAME));
		String jsonform = m.writeValueAsString(param);
		System.out.println("Request to be sent:\n" + jsonform);
		post.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
		HttpResponse responsePost = client.execute(host, post, localcontext);
		String contents = EntityUtils.toString(responsePost.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), responsePost.getStatusLine().getStatusCode());

		assertThat(contents, is("sync"));
		catchException(idsMan).getEntity(new EntityParam(identityParam));
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
}
