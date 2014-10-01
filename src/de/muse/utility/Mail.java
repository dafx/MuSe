/*
 * Copyright (C) 2014 University of Freiburg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.muse.utility;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import de.muse.config.ApplicationConfig;

/**
 * Helper class to send mails
 */
public class Mail {
	private static final Properties PROPS;

	static {
		// Initialize properties on class load
		PROPS = System.getProperties();
		PROPS.put("mail.smtp.host", ApplicationConfig.MAIL_HOST);
		PROPS.put("mail.smtp.port", "25");
	}

	private final String reciever;
	private final String content;

	/**
	 * Create a new mail object with message content and receiver email address.
	 * 
	 * @param reciever
	 *            Email address of the receiver
	 * @param content
	 *            Message to send
	 */
	public Mail(String reciever, String content) {
		this.reciever = reciever;
		this.content = content;
	}

	/**
	 * Send the mail specified in the object.
	 */
	public void send() throws AddressException, MessagingException {
		// Specify sender
		Session session = Session.getDefaultInstance(PROPS);
		MimeMessage message = new MimeMessage(session);

		message.setFrom(new InternetAddress(ApplicationConfig.MAIL_SENDER));
		// Specify reciever
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(
				reciever));

		// Specify content
		message.setSubject("Password recovery");
		message.setContent(content, "text/html; charset=utf-8");
		Transport.send(message);
	}
}
