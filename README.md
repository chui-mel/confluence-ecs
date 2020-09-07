# CDK for Confluence sServer ECS

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

## Required Env locally

Refer to [AWS CDK Toolkit (cdk command)](https://docs.aws.amazon.com/cdk/latest/guide/cli.html)
1. install nodejs (better to use the latest stable one)
2. install AWS CDK `npm install -g aws-cdk`
3. configure your aws token locally by using aws cli

## deployment process:
1. `mvn package`
2. `cdk synth`
3. `cdk deploy'

## How to get public IP for ECS task
- install jq 
- run the following command to get public ip list with vpcId and eniId
  if you have only one task running, then that public ip is you need.
  ```
   aws ec2 describe-network-interfaces | jq '.NetworkInterfaces | .[] | (.NetworkInterfaceId, .VpcId, .Association.PublicIp)'
  ```
- If you want to get more information about the ECS tasks, run the following command
    ```
    aws ecs list-clusters | jq '.clusterArns' | grep ConfluenceEcsStack
  
    aws ecs list-tasks --cluster ConfluenceEcsStack-ConfluenceClusterDE24D643-N2QyEvSMDuwJ | jq '.taskArns'
    
    aws ecs describe-tasks --cluster ConfluenceEcsStack-ConfluenceClusterDE24D643-N2QyEvSMDuwJ --tasks 78582e64-17ec-4a14-a471-0f07d9955d27 | jq '.tasks | .[0] | .attachments| .[0] | .details'
    ```
  
Note: 'ConfluenceEcsStack-ConfluenceClusterDE24D643-N2QyEvSMDuwJ' and '78582e64-17ec-4a14-a471-0f07d9955d27' are examples for the command returns.

## Watch out when deleting cloudformation stack
- when you finish deleting cloudformation stack (whatever way), remember to check cloudwatch log group, 
because the log group retains longer than your expectation. They might be there and fail your next CDK deployment.