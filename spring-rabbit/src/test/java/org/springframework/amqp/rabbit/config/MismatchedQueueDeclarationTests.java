/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.rabbit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.StandardEnvironment;

import com.rabbitmq.client.Channel;

/**
 * @author Gary Russell
 * @author Gunnar Hillert
 * @since 1.2
 *
 */
public class MismatchedQueueDeclarationTests {

	@Rule
	public BrokerRunning brokerIsRunning = BrokerRunning.isRunning();

	private SingleConnectionFactory connectionFactory;

	private RabbitAdmin admin;

	@Before
	public void setup() throws Exception {
		connectionFactory = new SingleConnectionFactory();
		connectionFactory.setHost("localhost");
		this.admin = new RabbitAdmin(this.connectionFactory);
		deleteQueues();
	}

	@After
	public void deleteQueues() throws Exception {
		this.admin.deleteQueue("mismatch.foo");
		this.admin.deleteQueue("mismatch.bar");

		((DisposableBean) connectionFactory).destroy();
	}

	@Test
	@Ignore
	public void testAdminFailsWithMismatchedQueue() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setConfigLocation("org/springframework/amqp/rabbit/config/MismatchedQueueDeclarationTests-context.xml");
		StandardEnvironment env = new StandardEnvironment();
		env.addActiveProfile("basicAdmin");
		env.addActiveProfile("basic");
		context.setEnvironment(env);
		context.refresh();
		context.getBean(CachingConnectionFactory.class).createConnection();
		context.close();
		Channel channel = this.connectionFactory.createConnection().createChannel(false);
		channel.queueDeclarePassive("mismatch.bar");
		this.admin.deleteQueue("mismatch.bar");
		assertThat(this.admin.getQueueProperties("mismatch.foo")).isNotNull();
		assertThat(this.admin.getQueueProperties("mismatch.bar")).isNull();

		env = new StandardEnvironment();
		env.addActiveProfile("basicAdmin");
		env.addActiveProfile("ttl");
		context.setEnvironment(env);
		context.refresh();
		channel = this.connectionFactory.createConnection().createChannel(false);
		try {
			context.getBean(CachingConnectionFactory.class).createConnection();
			fail("Expected exception - basic admin fails with mismatched declarations");
		}
		catch (Exception e) {
			assertThat(e.getCause().getCause().getMessage().contains("inequivalent arg 'x-message-ttl'")).isTrue();
		}
		assertThat(this.admin.getQueueProperties("mismatch.foo")).isNotNull();
		assertThat(this.admin.getQueueProperties("mismatch.bar")).isNull();
		context.close();
	}

	@Test
	public void testAdminSkipsMismatchedQueue() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setConfigLocation("org/springframework/amqp/rabbit/config/MismatchedQueueDeclarationTests-context.xml");
		StandardEnvironment env = new StandardEnvironment();
		env.addActiveProfile("advancedAdmin");
		env.addActiveProfile("basic");
		context.setEnvironment(env);
		context.refresh();
		context.getBean(CachingConnectionFactory.class).createConnection();
		context.close();
		Channel channel = this.connectionFactory.createConnection().createChannel(false);
		channel.queueDeclarePassive("mismatch.bar");
		this.admin.deleteQueue("mismatch.bar");
		assertThat(this.admin.getQueueProperties("mismatch.foo")).isNotNull();
		assertThat(this.admin.getQueueProperties("mismatch.bar")).isNull();

		context = new ClassPathXmlApplicationContext();
		context.setConfigLocation("org/springframework/amqp/rabbit/config/MismatchedQueueDeclarationTests-context.xml");
		env = new StandardEnvironment();
		env.addActiveProfile("advancedAdmin");
		env.addActiveProfile("ttl");
		context.setEnvironment(env);
		context.refresh();
		channel = this.connectionFactory.createConnection().createChannel(false);
		context.getBean(CachingConnectionFactory.class).createConnection();
		assertThat(this.admin.getQueueProperties("mismatch.foo")).isNotNull();
		assertThat(this.admin.getQueueProperties("mismatch.bar")).isNotNull();
		context.close();
	}

}
