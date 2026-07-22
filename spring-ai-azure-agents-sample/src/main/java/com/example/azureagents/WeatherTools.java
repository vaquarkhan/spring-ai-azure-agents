package com.example.azureagents;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Demo tools for the sample agent.
 *
 * @author Viquar Khan
 */
@Component
public class WeatherTools {

	@Tool(description = "Get the current weather for a city")
	public String getWeather(
			@ToolParam(description = "City and optional state/country, e.g. Seattle, WA") String location) {
		return "{\"location\":\"" + location + "\",\"temperature_c\":18,\"conditions\":\"partly cloudy\"}";
	}

}
