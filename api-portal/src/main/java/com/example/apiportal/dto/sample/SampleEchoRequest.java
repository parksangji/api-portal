package com.example.apiportal.dto.sample;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SampleEchoRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;
}