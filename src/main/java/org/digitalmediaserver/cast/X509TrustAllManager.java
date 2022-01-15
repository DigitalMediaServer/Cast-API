/*
 * Copyright 2014 Vitaly Litvak (vitavaque@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.cast;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


/**
 * Google Cast certificates cannot be validated against a standard keystore, so
 * use a dummy trust-all manager.
 */
public class X509TrustAllManager extends X509ExtendedTrustManager {

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] certs, String authType) {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] certs, String authType) {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
	}
}
