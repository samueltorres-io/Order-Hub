package com.orderhub.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @RestController
    static class DummyController {
        @GetMapping("/test/app-exception")
        public void throwAppException() {
            throw new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        @GetMapping("/test/generic-exception")
        public void throwGenericException() {
            throw new RuntimeException("Unexpected database error");
        }

        @PostMapping("/test/validation")
        public void testValidation(@RequestBody @Valid DummyDto dto) {}
    }

    @Data
    static class DummyDto {
        @NotNull(message = "cannot be null")
        private String field;
    }

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    @DisplayName("Should handle AppException correctly")
    void handleAppException() throws Exception {
        mockMvc.perform(get("/test/app-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is(ErrorCode.USR_NOT_FOUND.getCode())))
                .andExpect(jsonPath("$.message", is(ErrorCode.USR_NOT_FOUND.getDefaultMessage())))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("Should handle Generic Exception hiding details in PROD")
    void handleUnexpected_Prod() throws Exception {
        ReflectionTestUtils.setField(globalExceptionHandler, "activeProfile", "prod");

        mockMvc.perform(get("/test/generic-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode", is(ErrorCode.INTERNAL_SERVER_ERROR.getCode())))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.message", containsString("contact support")));
    }

    @Test
    @DisplayName("Should handle Generic Exception showing details in DEV")
    void handleUnexpected_Dev() throws Exception {

        ReflectionTestUtils.setField(globalExceptionHandler, "activeProfile", "dev");

        mockMvc.perform(get("/test/generic-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode", is(ErrorCode.INTERNAL_SERVER_ERROR.getCode())))
                .andExpect(jsonPath("$.details", is("Unexpected database error")));
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void handleValidation() throws Exception {

        ReflectionTestUtils.setField(globalExceptionHandler, "activeProfile", "prod");

        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is(ErrorCode.INVALID_INPUT.getCode())))
                .andExpect(jsonPath("$.message", is("Invalid request data. Please check your input and try again.")));
    }

    @Test
    @DisplayName("Should handle Malformed JSON")
    void handleMalformedJson() throws Exception {
        ReflectionTestUtils.setField(globalExceptionHandler, "activeProfile", "prod");

        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"field\": \"valor cortad...")) 
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is(ErrorCode.MALFORMED_JSON.getCode())))
                .andExpect(jsonPath("$.message", containsString("Invalid JSON")));
    }
}