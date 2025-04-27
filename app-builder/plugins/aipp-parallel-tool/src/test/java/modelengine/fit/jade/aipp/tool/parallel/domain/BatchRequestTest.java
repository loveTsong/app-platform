/*
 * Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 * This file is a part of the ModelEngine Project.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package modelengine.fit.jade.aipp.tool.parallel.domain;

import static org.junit.jupiter.api.Assertions.*;

import modelengine.fit.jade.aipp.tool.parallel.entities.Argument;
import modelengine.fit.jade.aipp.tool.parallel.entities.Config;
import modelengine.fit.jade.aipp.tool.parallel.entities.ToolCall;
import modelengine.fit.jade.aipp.tool.parallel.support.TaskExecutor;
import modelengine.fit.jade.tool.SyncToolCall;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class BatchRequestTest {
    @Mock
    private SyncToolCall syncToolCall;

    @Mock
    private TaskExecutor taskExecutor;

    @Test
    void shouldCallExecutorByConcurrencyWhenPostGivenToolCall() {
        List<ToolCall> toolCalls = Arrays.asList(ToolCall.builder().uniqueName("u1").args(new ArrayList<>()).build(),
                ToolCall.builder().uniqueName("u2").args(new ArrayList<>()).build(),
                ToolCall.builder().uniqueName("u3").args(new ArrayList<>()).build());
        Config config = Config.builder().concurrency(2).build();
        Mockito.doNothing().when(this.taskExecutor).post(Mockito.any());

        BatchRequest batchRequest = new BatchRequest(toolCalls, config, this.syncToolCall, this.taskExecutor);
        batchRequest.post();

        Mockito.verify(this.taskExecutor, Mockito.times(config.getConcurrency())).post(Mockito.any());
    }

    @Test
    void shouldGetResultWhenAwaitGivenToolCallSuccessfully() {
        List<ToolCall> toolCalls = Arrays.asList(ToolCall.builder().uniqueName("u1").args(new ArrayList<>()).build(),
                ToolCall.builder()
                        .uniqueName("u2")
                        .args(Collections.singletonList(Argument.builder().name("a").value(1).build()))
                        .build());
        Config config = Config.builder().concurrency(1).build();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(this.taskExecutor).post(Mockito.any());
        Mockito.when(this.syncToolCall.call(Mockito.eq(toolCalls.get(0).getUniqueName()), Mockito.eq("{}")))
                .thenReturn("1");
        Mockito.when(this.syncToolCall.call(Mockito.eq(toolCalls.get(1).getUniqueName()), Mockito.eq("{\"a\":1}")))
                .thenReturn("\"2\"");

        BatchRequest batchRequest = new BatchRequest(toolCalls, config, this.syncToolCall, this.taskExecutor);
        batchRequest.post();
        List<Object> result = batchRequest.await();

        Mockito.verify(this.taskExecutor, Mockito.times(toolCalls.size())).post(Mockito.any());
        Assertions.assertEquals(toolCalls.size(), result.size());
        Assertions.assertInstanceOf(Integer.class, result.get(0));
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertInstanceOf(String.class, result.get(1));
        Assertions.assertEquals("2", result.get(1));
    }

    @Test
    void shouldThrowExceptionWhenAwaitGivenToolCallException() {
        List<ToolCall> toolCalls = Arrays.asList(ToolCall.builder().uniqueName("u1").args(new ArrayList<>()).build(),
                ToolCall.builder().uniqueName("u2").args(new ArrayList<>()).build());
        Config config = Config.builder().concurrency(1).build();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(this.taskExecutor).post(Mockito.any());
        Mockito.when(this.syncToolCall.call(Mockito.eq(toolCalls.get(0).getUniqueName()), Mockito.eq("{}")))
                .thenThrow(new IllegalArgumentException("wrong argument"));

        BatchRequest batchRequest = new BatchRequest(toolCalls, config, this.syncToolCall, this.taskExecutor);
        batchRequest.post();
        IllegalStateException exception = assertThrows(IllegalStateException.class, batchRequest::await);

        Assertions.assertTrue(exception.getMessage().endsWith("uniqueName=u1, index=0, errorMessage=wrong argument]"));
    }
}