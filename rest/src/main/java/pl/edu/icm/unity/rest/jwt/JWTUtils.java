/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import eu.emi.security.authn.x509.X509Credential;

/**
 * Simplifies JWT creation and parsing.
 * @author K. Benedyczak
 */
public class JWTUtils
{
	public static final Set<String> REQUIRED_CLAIMS = new HashSet<String>();
	static 
	{
		Collections.addAll(REQUIRED_CLAIMS, "iss", "sub", "aud", "exp", "iat", "jti");
	}
	
	public static String generate(X509Credential signingCred, String subject, String issuer, String audience,
			Date expires, String id) throws JOSEException
	{
		PrivateKey pk = signingCred.getKey();
		JWSSigner signer;
		if (pk instanceof RSAPrivateKey)
			signer = new RSASSASigner((RSAPrivateKey) signingCred.getKey());
		else
			throw new IllegalArgumentException("The credential for signing JWT must be of RSA type.");

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().
				subject(subject).
				issueTime(new Date()).
				issuer(issuer).
				audience(audience).
				expirationTime(expires).
				jwtID(id).build();

		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
		signedJWT.sign(signer);
		return signedJWT.serialize();
	}
	
	public static String generate(X509Credential signingCred, JWTClaimsSet claimsSet) throws JOSEException
	{
		PrivateKey pk = signingCred.getKey();
		JWSSigner signer;
		if (pk instanceof RSAPrivateKey)
			signer = new RSASSASigner((RSAPrivateKey) signingCred.getKey());
		else
			throw new IllegalArgumentException("The credential for signing JWT must be of RSA type.");

		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
		signedJWT.sign(signer);
		return signedJWT.serialize();
	}
	
	/**
	 * Parses the JWT token, verifies the signature, validity and claims completeness.
	 * @param token
	 * @param signingCred
	 * @return parsed and verified claims
	 * @throws ParseException
	 * @throws JOSEException
	 */
	public static JWTClaimsSet parseAndValidate(String token, X509Credential signingCred) 
			throws ParseException, JOSEException
	{
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) signingCred.getCertificate().getPublicKey());
		if (!signedJWT.verify(verifier))
		{
			throw new JOSEException("JWT signature is invalid");
		}
		
		JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
		if (new Date().after(claims.getExpirationTime()))
		{
			throw new JOSEException("JWT is expired");
		}
		validateClaimsSet(claims);
		return claims;
	}
	
	
	private static void validateClaimsSet(JWTClaimsSet claims) throws ParseException
	{
		Set<String> keys = claims.getClaims().keySet();
		if (!keys.containsAll(REQUIRED_CLAIMS))
			throw new ParseException("The claims in the JWT are incomplete", 0);
	}
}
