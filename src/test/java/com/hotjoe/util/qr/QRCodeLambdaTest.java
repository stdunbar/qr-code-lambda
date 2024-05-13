package com.hotjoe.util.qr;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Events;
import com.amazonaws.services.lambda.runtime.tests.annotations.HandlerParams;
import com.amazonaws.services.lambda.runtime.tests.annotations.Responses;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class QRCodeLambdaTest {

    @ParameterizedTest
    @HandlerParams(
            events = @Events(folder = "apigw/events/", type = APIGatewayProxyRequestEvent.class),
            responses = @Responses(folder = "apigw/responses/", type = APIGatewayProxyResponseEvent.class)
    )
    public void testMultipleEventsResponsesInFolder(APIGatewayProxyRequestEvent event, APIGatewayProxyResponseEvent response) {
        QRCodeLambda handler = new QRCodeLambda();


        APIGatewayProxyResponseEvent result = handler.handleRequest(event, new MockContext());

        assertThat(result.getStatusCode()).isEqualTo(response.getStatusCode());
        assertThat(result.getBody()).isEqualTo(response.getBody());
        assertThat(result.getIsBase64Encoded()).isEqualTo(response.getIsBase64Encoded());
        assertThat(result.getHeaders().get("Content-Type")).isEqualTo(response.getHeaders().get("Content-Type"));

    }

    public static class MockLambdaLogger implements LambdaLogger {

        @Override
        public void log(String s) {
            System.out.println(s);
        }

        @Override
        public void log(byte[] bytes) {
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    public static class MockContext implements Context {
        LambdaLogger logger = new MockLambdaLogger();

        @Override
        public String getAwsRequestId() {
            return "";
        }

        @Override
        public String getLogGroupName() {
            return "";
        }

        @Override
        public String getLogStreamName() {
            return "";
        }

        @Override
        public String getFunctionName() {
            return "";
        }

        @Override
        public String getFunctionVersion() {
            return "";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 0;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 0;
        }

        @Override
        public LambdaLogger getLogger() {
            return logger;
        }
    }

}
