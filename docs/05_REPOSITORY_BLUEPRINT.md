# Forge AI Platform
## Repository Blueprint

**Version**

1.0

## Philosophy

A repository should be understandable within 5 minutes.

Anyone should know:

where code lives
where documentation lives
where architecture lives
where APIs live
where tests live
where AI prompts live
where workflows live

without asking questions.

## Golden Rule

Every folder must answer ONE question.

Never create folders like

utils
misc
helpers
common2
new
latest
temp

Those destroy architecture.

## Repository Structure

forge-ai/

│

├── docs/

├── platform/

├── applications/

├── services/

├── sdk/

├── agents/

├── prompts/

├── workflows/

├── infrastructure/

├── scripts/

├── testing/

├── examples/

├── assets/

├── tools/

├── .github/

└── README.md

## Why?

Every directory has one responsibility.

### docs/

Everything humans read.

Contains

Architecture

Roadmap

Design Decisions

Requirements

Security

API

Database

Release Notes

ADR

Contributing Guide

Never code.

### platform/

The heart.

Contains

Platform Core

Events

Configuration

Logging

Security

Observability

Shared Components

Framework

Platform Kernel

Everything shared starts here.

### applications/

Real products.

Example

Forge Dashboard

Forge API

Forge CLI

Forge Desktop

Forge VS Code

Forge Web

Applications

never contain business logic.

They only compose services.

### services/

Business modules.

Example

Identity

Repository

Knowledge

Analytics

Automation

AI

Security

Notification

Workspace

Observability

Every service owns its business logic.

### sdk/

Everything developers use.

Java SDK

Python SDK

TypeScript SDK

CLI SDK

Plugin SDK

Future ready.

### agents/

The future.

Every AI Agent

has its own folder.

Example

Repository Agent

Review Agent

Security Agent

Performance Agent

Architecture Agent

Planning Agent

Documentation Agent

Every agent

is independently testable.

### prompts/

Huge mistake

most AI projects make.

Prompts are hardcoded.

Never do that.

Store

System Prompts

Planning Prompts

Review Prompts

Security Prompts

Architecture Prompts

Evaluation Prompts

Templates

Prompt Versions

Prompts are assets.

Not code.

### workflows/

Contains

engineering workflows.

Example

Review PR

Analyze Repository

Generate Documentation

Incident Analysis

Security Scan

Release Workflow

Architecture Review

Think

GitHub Actions

Temporal

Agent workflows

### infrastructure/

Everything deployment.

Docker

Kubernetes

Terraform

Helm

Cloud

Compose

Monitoring

Ingress

Networking

### scripts/

Developer utilities.

Bootstrap

Migration

Seed

Backup

Cleanup

Generate

Release

### testing/

Never mix testing

with production.

Contains

Integration

Performance

Security

Load

Chaos

Contract

E2E

Test Data

### examples/

Shows developers

how to use Forge AI.

### tools/

Internal engineering tools.

Example

Prompt Validator

Architecture Checker

Schema Generator

API Generator

Dependency Visualizer

### .github/

Professional projects

invest heavily here.

Contains

Issue Templates

PR Templates

CODEOWNERS

Workflows

Discussions

Security Policy

## Documentation Hierarchy

One day

Forge AI may have

100+

documents.

Organize

docs/

01 Vision

02 Requirements

03 Architecture

04 Domain

05 Repository

06 Database

07 APIs

08 Security

09 AI

10 Deployment

11 Testing

12 Coding Standards

13 Release

14 ADR

15 Roadmap

Professional.

Predictable.

## Naming Rules

Never abbreviate.

Bad

svc

util

cfg

authsvc

repo2

common

Good

Identity Service

Repository Service

Knowledge Service

Observability Platform

Readable code scales better than clever names.

## README Rule

Every folder

must eventually contain

its own README.

Every engineer

should understand

that folder

without reading code.

## Future Growth

This repository should comfortably support

100 developers

500,000+

Lines of Code

Multiple Products

Enterprise Edition

Cloud Edition

Community Edition

without restructuring.

## Architectural Rule

Repository Structure

must reflect

Business Structure.

Never

Framework Structure.

Wrong

controller

service

dao

entity

dto

That is a framework layout.

Right

Identity

Repository

Knowledge

Automation

AI

Observability

Inside each domain

then

controller

application

domain

infrastructure

This is called a vertical slice or feature-first architecture, and it helps keep business capabilities cohesive as the project grows.

## The Biggest Improvement

After reflecting on everything we've designed, I want to make one significant change to our overall process.

We should not ask GitHub Copilot to generate this project module by module immediately.

Instead, before writing any production code, we should spend time designing:

the coding standards,
architecture decision records (ADRs),
API standards,
testing strategy,
security standards,
AI prompt standards,
and definition of done.

Why?

Because AI is very good at generating code, but it is much more consistent when it is guided by stable engineering standards. Those standards become the "guardrails" that keep the codebase coherent over time.

## My revised roadmap

I believe the order should now be:

Vision ✅
Product Requirements ✅
System Architecture ✅
Domain Model ✅
Repository Blueprint ✅
Coding Standards
Architecture Decision Records (ADR)
Definition of Done
API Standards
Database Standards
Testing Strategy
Security Standards
AI Prompt Standards
Finally... start writing code.

This is slower than jumping into implementation, but it mirrors how mature engineering organizations establish a long-lived platform. It will also make GitHub Copilot much more effective because it will have a clear, stable specification to follow for every module.
