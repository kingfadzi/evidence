# Compliance Evidence Collector API

A rule-based engine to automate the collection of compliance evidence from cloud and container platforms.

## Compliance Matrix

This table maps compliance requirements to their respective data sources across different platforms.

| Main Category | Rule Set Field | OpenShift Data Source | VMware Data Source | AWS Data Source |
| :--- | :--- | :--- | :--- | :--- |
| **confidentiality_rating** | Confidentiality Level | --- | --- | --- |
| | Data Residency Control | --- | --- | --- |
| | De-Identification | --- | --- | --- |
| | Access Review Cadence | --- | --- | --- |
| | Third-Party Attestation | --- | --- | --- |
| | Data Retention Policy | --- | --- | --- |
| | Secure Data Deletion Evidence | --- | --- | --- |
| **integrity_rating** | Data Validation | --- | --- | --- |
| | Reconciliation Frequency | --- | --- | --- |
| | Audit Logging | --- | --- | --- |
| | Change Control | - | vCenter Change Management records | - |
| | Immutability Required | --- | --- | --- |
| | Log Retention Period | --- | --- | --- |
| **security_rating** | Encryption at Rest | Kubernetes Secrets, PVC encryption | VM Encryption settings, vSAN encryption | KMS encryption for EBS, S3, RDS, DynamoDB |
| | Encryption in Transit | Network policies, Service Mesh mTLS | NSX firewall rules, overlay network encryption | ELB/ALB, CloudFront, TLS enforcement |
| | Security Testing | --- | --- | --- |
| | Secrets Management | Kubernetes Secrets, External Secrets Operator | vSphere Secrets, External Vault integration | AWS Secrets Manager, Parameter Store |
| | Key Rotation Max | - | - | KMS Customer Managed Keys metadata |
| | Multi-Factor Authentication | - | - | IAM MFA, AWS SSO policies |
| | Privileged Access Management | RBAC policies | vCenter RBAC and permissions | IAM roles/policies, AWS Access Analyzer |
| | Patch Remediation SLA | Update Service status, ClusterOperator health | Update Manager patch compliance reports | SSM Patch Manager, Inspector, Systems Manager |
| | Dependency/SBOM Management | --- | --- | --- |
| | Network Segmentation Evidence | NetworkPolicy resources | NSX distributed firewall, segmentation policies | Security Groups, NACLs, VPC/subnet configs |
| | Web Application Firewall Evidence | Ingress controllers, WAF configs | NSX Edge WAF configurations (if deployed) | AWS WAF, ALB/WAF associations |
| | SIEM / Central Log Integration | Cluster logging with Fluentd | Log Insight, vRealize Log Analytics integration | CloudTrail, CloudWatch Logs, Firehose, EventBridge |
| **availability_rating** | RTO (hours) | --- | --- | --- |
| | RPO (minutes) | --- | --- | --- |
| | HA Topology | Cluster node status, control plane config | vSphere HA cluster configuration | Multi-AZ setup, Auto Scaling, ELB configs |
| | Monitoring SLOs | Prometheus metrics, alerting | vRealize Ops metrics and alerts | CloudWatch Alarms, Service Health Dashboard |
| | On-call Coverage | --- | --- | --- |
| **resilience_rating** | DR Test Frequency | --- | --- | --- |
| | Backup Policy | Backup operator configs, Velero status | Data Protection, VCF backup configs | AWS Backup, RDS/S3/EBS backup status |
| | Failover Automation | Operator health, failover settings | HA & Site Recovery Manager configs | Route53 health checks, Multi-AZ configs |
| | Runbook Maturity | --- | --- | --- |
| | Chaos Testing | --- | --- | --- |
| | Incident Response Plan | --- | --- | --- |
| | Incident Response Exercise | --- | --- | --- |

## Architecture

### Schema Overview

Evidence collection is defined in YAML files (e.g., `aws-ruleset.yml`, `kubernetes-ruleset.yml`). These files are validated against a JSON schema (`src/main/resources/schemas/ruleset.schema.json`) which enables IDE autocompletion and validation.

A rule defines what to collect and, optionally, how to assess it for compliance.

**Example Rule (`aws-ruleset.yml`):**
```yaml
# yaml-language-server: $schema=./schemas/ruleset.schema.json
rules:
  - id: "aws-kms-key-rotation"
    name: "Key Rotation Max"
    platform: "aws"
    service: "kms"
    description: "Verifies that customer-managed KMS keys have automatic rotation enabled."
    collection:
      apiCall: "getKeyRotationStatus"
      parameters:
        keyId: "${arn}"
      responseField: "keyRotationEnabled"
  assessment:
    compliantWhen:
      isTrue: true
```

### Rule Engine

The `GenericRuleBasedProvider` is the core of the application. It is a platform-agnostic service that parses the YAML rules and orchestrates the evidence collection by delegating to the appropriate `PlatformHandler`.

### Platform Handlers

Platform Handlers are the "plug-ins" that contain all platform-specific logic. To add support for a new platform, you must create a class that implements the `PlatformHandler` interface.

**`PlatformHandler.java` Interface:**
```java
package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.rules.Rule;

public interface PlatformHandler {
    String getPlatformName();
    Object getClient(String service, PlatformInstanceConfig config);
    Object buildApiRequest(Rule rule, String resourceIdentifier);
    String parseServiceFromIdentifier(String resourceIdentifier);
}
```

**Current Implementations:**
*   `AwsPlatformHandler.java`
*   `KubernetesPlatformHandler.java`

## Quick Start

### 1. Configure

Modify `src/main/resources/application.yml` to define your platform instances. Credentials should be supplied via environment variables, not hardcoded.

```yaml
compliance-collector:
  platforms:
    instances:
      aws:
        - name: "aws-prod-account"
          properties:
            region: "us-east-1"
      kubernetes:
        - name: "k8s-dev-cluster"
          properties:
            kubeconfig: "/path/to/your/.kube/config"
            context: "k3d-default"
```

### 2. Run

```bash
./mvnw spring-boot:run
```

### 3. Collect Evidence

Use the `/api/v1/collect` endpoint to trigger evidence collection.

```bash
curl -X POST -H "Content-Type: application/json" \
-d 
{
  "applicationId": "my-critical-app-123",
  "platformName": "aws-prod-account",
  "resourceArns": [
    "arn:aws:kms:us-east-1:123456789012:key/YOUR_KEY_UUID"
  ],
  "ruleSetFields": [
    "Key Rotation Max"
  ]
}
\
http://localhost:8080/api/v1/collect
```

