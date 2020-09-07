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

        Vpc vpc = Vpc.Builder.create(this, "ConfluenceVpc")
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

        Cluster cluster = Cluster.Builder.create(this, "ConfluenceCluster")
            .vpc(vpc)
            .build();

//        setupService("ConfluenceServer772", "confluence-772-test", "atlassian/confluence-server:7.7.2",
//            cluster, List.of(securityGroup));
//        setupService("ConfluenceServer773", "confluence-773-test", "atlassian/confluence-server:7.7.3",
//            cluster, List.of(securityGroup));
//
        stopTaskInService("ConfluenceServer772", "confluence-772-test", "atlassian/confluence-server:7.7.2",
            cluster, List.of(securityGroup));
        stopTaskInService("ConfluenceServer773", "confluence-773-test", "atlassian/confluence-server:7.7.3",
            cluster, List.of(securityGroup));
    }

    private FargateService setupService(String serviceId, String taskId, String imageName,
                                        Cluster cluster, List<ISecurityGroup> sgs) {
        return FargateService.Builder.create(this, serviceId)
            .cluster(cluster)
            .securityGroups(sgs)
            .taskDefinition(defineTask(taskId, imageName))
            .desiredCount(1)
            .assignPublicIp(true)
            .build();
    }

    private FargateService stopTaskInService(String serviceId, String taskId, String imageName,
                                        Cluster cluster, List<ISecurityGroup> sgs) {
        return FargateService.Builder.create(this, serviceId)
            .cluster(cluster)
            .securityGroups(sgs)
            .taskDefinition(defineTask(taskId, imageName))
            .desiredCount(0)
            .assignPublicIp(true)
            .build();
    }

    private FargateTaskDefinition defineTask(String taskId, String imageName) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
            .create(this, "confluence-test" + taskId)
            .memoryLimitMiB(4096)
            .cpu(2048)
            .build();

        ContainerDefinition containerDefinition = taskDefinition.addContainer("confluence-" + taskId ,
            ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                    .streamPrefix("confluence-" + taskId)
                    .logGroup(LogGroup.Builder.create(this, "confluence-log-" + taskId).logGroupName("ecs-confluence" + taskId).build())
                    .build()))
                .build());

        containerDefinition.addPortMappings(
            PortMapping.builder().containerPort(8090).hostPort(8090).build(),
            PortMapping.builder().containerPort(8091).hostPort(8091).build()
        );

        return taskDefinition;
    }
}
