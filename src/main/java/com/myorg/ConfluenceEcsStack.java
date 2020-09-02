package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.logs.LogGroup;

import java.util.List;

public class ConfluenceEcsStack extends Stack {
    public ConfluenceEcsStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ConfluenceEcsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "MyVpc")
            .maxAzs(2)  // Default is all AZs in region
            .natGateways(0)
            .subnetConfiguration(List.of(
                SubnetConfiguration.builder().name("public-sn")
                    .subnetType(SubnetType.PUBLIC).cidrMask(24).build()))
            .build();

        SecurityGroup securityGroup = SecurityGroup.Builder.create(this, "ingress-sg")
            .vpc(vpc)
            .allowAllOutbound(true)
            .build();

        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8090));
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8091));

        Cluster cluster = Cluster.Builder.create(this, "MyCluster")
            .vpc(vpc)
            .build();

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
            .create(this, "confluence-test")
            .memoryLimitMiB(4096)
            .cpu(2048)
            .build();

        ContainerDefinition containerDefinition = taskDefinition.addContainer("confluence-container",
            ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("atlassian/confluence-server"))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                    .streamPrefix("confluence-cluster")
                    .logGroup(LogGroup.Builder.create(this, "confluence-test-log").logGroupName("ecs-confluence").build())
                    .build()))
                .build());

        containerDefinition.addPortMappings(
            PortMapping.builder().containerPort(8090).hostPort(8090).build(),
            PortMapping.builder().containerPort(8091).hostPort(8091).build()
        );

        FargateService.Builder.create(this, "ConfluenceTestService")
            .cluster(cluster)
            .securityGroups(List.of(securityGroup))
            .taskDefinition(taskDefinition)
            .desiredCount(1)
            .assignPublicIp(true)
            .build();

    }
}
