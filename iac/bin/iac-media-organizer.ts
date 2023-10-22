#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { IacMediaOrganizerStack } from "../lib/iac-media-organizer-stack";

const app = new cdk.App();
new IacMediaOrganizerStack(app, "IacMediaOrganizerStack", {});
