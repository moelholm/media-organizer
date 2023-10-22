import * as cdk from "aws-cdk-lib";
import {Construct} from "constructs";
import * as dynamodb from "aws-cdk-lib/aws-dynamodb";
import {BillingMode} from "aws-cdk-lib/aws-dynamodb";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as iam from "aws-cdk-lib/aws-iam";
import * as events from "aws-cdk-lib/aws-events";
import * as targets from "aws-cdk-lib/aws-events-targets";

export class IacMediaOrganizerStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);
        //
        // Function: Runs the application
        //
        const lambdaFunction = this.createFunction(
            "mediaorganizerfunction",
            "com.moelholm.tools.aws.AwsLambdaHandler::handleRequest",
            "../app/target/media-organizer-0.0.1-SNAPSHOT.jar"
        );
        const newAccountsRule = new events.Rule(this, "mediaorganizerfunctionrule", {
            schedule: events.Schedule.cron({minute: "30", hour: "3"}),
            targets: [new targets.LambdaFunction(lambdaFunction)],
        });
    }

    createFunction(
        id: string,
        handler: string,
        lambdaZipFile: string,
    ) {
        const lambdaFunction = new lambda.Function(this, id, {
            runtime: lambda.Runtime.JAVA_17,
            handler: handler,
            timeout: cdk.Duration.minutes(15),
            code: lambda.Code.fromAsset(lambdaZipFile),
            environment: {
                "SPRING_PROFILES_ACTIVE": "production",
            },
        });
        lambdaFunction.addToRolePolicy(
            new iam.PolicyStatement({
                actions: ["ssm:GetParametersByPath"],
                resources: [
                    `arn:aws:ssm:${this.region}:${this.account}:parameter/APPLICATION/MEDIA-ORGANIZER`,
                ],
            })
        );
        return lambdaFunction;
    }
}
