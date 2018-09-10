package com.infinityrefactoring.util.message;

import static java.util.logging.Level.FINEST;

import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.el.ELProcessor;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import com.infinityrefactoring.util.text.Expression;
import com.infinityrefactoring.util.text.ExpressionDefinitions.DollarCurlyBracket;
import com.infinityrefactoring.util.text.ExpressionException;

@Default
@RequestScoped
public class DefaultMessageInterpolator implements MessageInterpolator {

	@Inject
	private Logger logger;

	@Inject
	private MessageProvider messageProvider;

	@Inject
	private DollarCurlyBracket dollarCurlyBracket;

	@Inject
	private ELProcessor elProcessor;

	@Inject
	private Locale locale;

	@Override
	public MessageInterpolator add(String key, Object value) {
		elProcessor.defineBean(key, value);
		return this;
	}

	@Override
	public String get(String key) {
		return get(key, locale);
	}

	@Override
	public String get(String key, Locale locale) {
		if (key == null) {
			throw new IllegalArgumentException("The key must be not null");
		}

		Map<String, String> messages = messageProvider.getMessages(locale);
		String template = messages.get(key);

		if (template == null) {
			throw new IllegalArgumentException("Not found message for key: " + key);
		}
		return interpolate(key, template);
	}

	private String interpolate(String key, String template) {
		try {
			SortedMap<Expression, SortedSet<Integer>> expressions = dollarCurlyBracket.findAll(template, 0);
			String message = dollarCurlyBracket.interpolate(template, expressions, expression -> elProcessor.eval(expression.getSubExpression()));
			if (logger.isLoggable(FINEST)) {
				logger.finest(String.format("\nInteporlated message: {\n    key: \"%s\"\n    template: \"%s\"\n    message: \"%s\"\n}", key, template, message));
			}
			return message;
		} catch (RuntimeException ex) {
			throw new ExpressionException("Cannot be interpolate the message: " + key, ex);
		}
	}

}
