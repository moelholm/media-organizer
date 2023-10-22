package com.moelholm.tools.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.moelholm.tools.mediaorganizer.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwsLambdaHandler implements RequestHandler<String, String> {

    private final Logger logger = LoggerFactory.getLogger(AwsLambdaHandler.class);

    @Override
    public String handleRequest(String input, Context context) {
        logger.info("Lambda started");
        try {
            Main.main(new String[]{});
            return "Lambda finished successfully";
        } catch (Exception e) {
            logger.error("Error while running lambda handler", e);
            return "Lambda finished with error [%s] (see logs for details)".formatted(e.getMessage());
        } finally {
            logger.info("Lambda finished");
        }
    }
}