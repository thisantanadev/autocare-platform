package com.autocare.backend.common.error;

import org.springframework.http.HttpStatus;

public class AnalyticsUnavailableException extends ApiException {

	public AnalyticsUnavailableException() {
		super(HttpStatus.SERVICE_UNAVAILABLE, "ANALYTICS_UNAVAILABLE",
				"The analytics service is temporarily unavailable. Vehicle data is unaffected; try again later.");
	}
}
