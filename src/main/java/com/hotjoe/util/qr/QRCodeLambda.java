package com.hotjoe.util.qr;

import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.core.JsonProcessingException;
import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.SerializationFeature;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;


/**
 * This is a small AWS Lambda to generate a QR code for a URL.  The code takes in a simple JSON object to do this:
 *
 * <pre>
 *   {
 *     "url": "https%3A%2F%2Fwww.blah.com%2F%3Fparam1%3Dblech%26param2%3Dboo",
 *     "size": 200
 *   }
 * </pre>
 *
 * The URL *must* be URL encoded to prevent any weirdness when passing through as JSON.  The size parameter is an
 * *optional* integer between 50 and 1000 and will be the size in pixels of the square QR code.  There are multiple
 * ways to configure a default for the size.  The hard coded default is 200 pixels.  If there is an environment
 * variable named DEFAULT_SIZE then that will be used.  Note that the size passed in the request takes precedence
 * if it exists.
 * <br />
 * This Lambda is expected to be fronted by an API Gateway.  The HTTP version of the API Gateway has been tested
 * though either should work, in addition to the Lambda Function URL.
 * <br />
 * Note that at this time only the URL QR code type is supported.  Future versions could update this.
 * <br />
 * This Lambda returns an "image/png" content type or an error if there was an issue with generating the QR code.
 *
 */
@SuppressWarnings("unused")
public class QRCodeLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Configuration jsonPathConfiguration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build();
    private static final Integer DEFAULT_SIZE = 200;
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);


    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {

        try {
            context.getLogger().log("input: " + mapper.writeValueAsString(apiGatewayProxyRequestEvent));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String defaultSizeString = System.getenv("DEFAULT_SIZE");
        int size = DEFAULT_SIZE;

        if( defaultSizeString != null )
            size = Integer.parseInt(defaultSizeString);

        String requestBody = apiGatewayProxyRequestEvent.getBody();

        if( requestBody == null ) {
            return writeResponse("{\"error\":\"missing body\"}", 400);
        }

        String url = JsonPath.using(jsonPathConfiguration).parse(requestBody).read("$.url");
        Integer requestedSize = JsonPath.using(jsonPathConfiguration).parse(requestBody).read("$.size");

        if( requestedSize != null ) {
            size = requestedSize;
        }

        if( (size < 50) || (size > 1000) ) {
            return writeResponse("{\"error\":\"size must be between 50 and 1000\"}", 400);
        }

        if( url == null ) {
            return writeResponse("{\"error\":\"missing url\"}", 400);
        }

        url = URLDecoder.decode(url, StandardCharsets.UTF_8);

        try {

            QRCodeWriter barcodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix =
                    barcodeWriter.encode(url, BarcodeFormat.QR_CODE, size, size);

            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            return writeResponse(bufferedImage);
        }
        catch( WriterException writerException ) {
            return writeResponse("{\"error\":\"cannot generate qr code - " + writerException.getMessage() + "\"}", 500);
        }

    }

    /**
     * Simple convenience method to write the appropriate response back to API Gateway.
     *
     * @param jsonString - a JSON object to send back to API Gateway
     * @param errorCode - the HTTP error code to send
     *
     * @return an APIGatewayProxyResponseEvent that contains what to send back to API Gateway
     *
     */
    private APIGatewayProxyResponseEvent writeResponse(String jsonString, int errorCode)  {
        return new APIGatewayProxyResponseEvent()
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withIsBase64Encoded(false)
                .withBody(jsonString)
                .withStatusCode(errorCode);
    }

    /**
     * Sends back the BufferedImage to API Gateway.
     *
     * @param bufferedImage - the image to send.  It is converted to be a PNG.
     *
     * @return an APIGatewayProxyResponseEvent that contains what to send back to API Gateway
     *
     */
    private APIGatewayProxyResponseEvent writeResponse(BufferedImage bufferedImage)  {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            return  new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "image/png"))
                    .withBody(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()))
                    .withIsBase64Encoded(Boolean.TRUE)
                    .withStatusCode(200);
        }
        catch( IOException ioe ) {
            return writeResponse("{\"error\":\"cannot generate qr code - " + ioe.getMessage() + "\"}", 500);
        }
    }
}
