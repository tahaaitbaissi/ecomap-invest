package com.example.backend.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchTriggerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, MethodSecurityExceptionAdvice.class})
class BatchTriggerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JobLauncher jobLauncher;
    @MockitoBean
    private Job osmImportJob;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void trigger_startsJob() throws Exception {
        var exec = mock(JobExecution.class);
        var inst = mock(JobInstance.class);
        when(inst.getInstanceId()).thenReturn(5L);
        when(exec.getJobInstance()).thenReturn(inst);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(exec);

        mockMvc.perform(post("/api/v1/admin/batch/trigger"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void trigger_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/trigger")).andExpect(status().isForbidden());
    }
}
