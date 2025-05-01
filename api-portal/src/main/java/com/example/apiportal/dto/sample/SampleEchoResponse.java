package com.example.apiportal.dto.sample;

import java.time.Instant;

public record SampleEchoResponse(String received, Instant timestamp) {}