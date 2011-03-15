package com.ning.arecibo.aggregator.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@BindingAnnotation
public @interface AsynchUpdateWorkerBufferSize
{
	int DEFAULT = 1000;
	String PROPERTY_NAME = "arecibo.events.aggregator.asynchUpdateWorkerBufferSize";
}