/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.workflow.metrics.sla.calendar;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.Locale;

/**
 * @author Inácio Nery
 */
public interface WorkflowMetricsSLACalendar {

	public static final String DEFAULT_KEY = "default";

	public Duration getDuration(
		LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime);

	public String getKey();

	public LocalDateTime getOverdueLocalDateTime(
		LocalDateTime nowLocalDateTime, Duration remainingDuration);

	public String getTitle(Locale locale);

}