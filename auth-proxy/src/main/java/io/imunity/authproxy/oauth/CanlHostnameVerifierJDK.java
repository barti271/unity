/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy.oauth;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.helpers.ssl.HostnameToCertificateChecker;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.utils.Log;

/**
 * TODO - copied from oauth module
 * CANL based hostname verifier for JDK.
 */
class CanlHostnameVerifierJDK implements HostnameVerifier
{
	private ServerHostnameCheckingMode mode;
	private static final Logger log = Log.getLogger(Log.SECURITY, CanlHostnameVerifierJDK.class);
	
	CanlHostnameVerifierJDK(ServerHostnameCheckingMode mode)
	{
		this.mode = mode;
	}

	@Override
	public boolean verify(String hostname, SSLSession session)
	{
		if (mode == ServerHostnameCheckingMode.NONE)
			return true;
		
		HostnameToCertificateChecker checker = new HostnameToCertificateChecker();
		
		X509Certificate cert;
		Certificate[] serverChain;
		try
		{
			serverChain = session.getPeerCertificates();
		} catch (SSLPeerUnverifiedException e1)
		{
			return false;
		}
		if (serverChain == null || serverChain.length == 0)
			throw new IllegalStateException("JDK BUG? Got null or empty peer certificate array");
		if (!(serverChain[0] instanceof X509Certificate))
			throw new ClassCastException("Peer certificate should be " +
					"an X.509 certificate, but is " + serverChain[0].getClass().getName());
		cert = (X509Certificate) serverChain[0];

		try
		{
			if (!checker.checkMatching(hostname, cert))
				return handleMismatch(hostname, cert);
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	private boolean handleMismatch(String hostName, X509Certificate cert)
	{
		if (mode == ServerHostnameCheckingMode.FAIL)
			return false;
		else
		{
			String message = "The server hostname is not matching its certificate subject. This might mean that" +
					" somebody is trying to perform a man-in-the-middle attack by pretending to be" +
					" the server you are trying to connect to. However it is also possible that" +
					" the server uses a certificate which was not associated with its address." +
					" The server DNS name is: '" + hostName + "' and its certificate subject is: '" +
					X500NameUtils.getReadableForm(cert.getSubjectX500Principal()) + "'.";
			log.warn(message);
			return true;
		}
	}
}
