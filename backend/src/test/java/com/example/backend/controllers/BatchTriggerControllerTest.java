package com.example.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.testsupport.MethodSecurityExceptionAdvice;
import com.example.backend.testsupport.MethodSecurityTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.step.StepExecution;
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
    private JobExplorer jobExplorer;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void trigger_startsJob() throws Exception {
        var exec = mock(JobExecution.class);
        var inst = mock(JobInstance.class);
        when(inst.getId()).thenReturn(5L);
        when(exec.getJobInstance()).thenReturn(inst);
        when(exec.getId()).thenReturn(42L);
        when(exec.getStatus()).thenReturn(BatchStatus.STARTED);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(exec);

        mockMvc.perform(post("/api/v1/admin/batch/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobInstanceId").value(5L))
                .andExpect(jsonPath("$.jobExecutionId").value(42L))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void trigger_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/trigger")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void status_returnsAggregatedMetrics() throws Exception {
        var ex = mock(JobExecution.class);
        var s1 = mock(StepExecution.class);
        when(s1.getReadCount()).thenReturn(10L);
        when(s1.getWriteCount()).thenReturn(8L);
        when(s1.getSkipCount()).thenReturn(2L);
        when(ex.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(ex.getStartTime())
                .thenReturn(java.time.LocalDateTime.parse("2025-01-01T10:00:00"));
        when(ex.getEndTime())
                .thenReturn(java.time.LocalDateTime.parse("2025-01-01T10:05:00"));
        when(ex.getStepExecutions()).thenReturn(List.of(s1));
        when(jobExplorer.getJobExecution(eq(99L))).thenReturn(ex);

        mockMvc.perform(get("/api/v1/admin/batch/status/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.readCount").value(10L))
                .andExpect(jsonPath("$.writeCount").value(8L))
                .andExpect(jsonPath("$.skipCount").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void status_notFound() throws Exception {
        when(jobExplorer.getJobExecution(eq(1L))).thenReturn(null);
        mockMvc.perform(get("/api/v1/admin/batch/status/1")).andExpect(status().isNotFound());
    }
}
