# Jenkins Setup and Build Steps

## 1) Start Jenkins

From `easyops-erp/jenkins`:

```bash
docker compose up -d
```

Open Jenkins at `http://localhost:18088`.

This setup now auto-bootstraps a Jenkins pipeline job on first start.

Before first run, update `docker-compose.yml`:

- `JENKINS_BOOTSTRAP_GITHUB_URL` -> your real GitHub repo URL
- Optional: `JENKINS_BOOTSTRAP_CREDENTIALS_ID` if private repo access is needed

## 2) Initial Jenkins configuration

1. Create credentials for GitHub repository access if needed (PAT or SSH key).
2. If you created credentials, set `JENKINS_BOOTSTRAP_CREDENTIALS_ID` in `docker-compose.yml`.
3. Recreate Jenkins container after env changes:
   - `docker compose down`
   - `docker compose up -d --build`
4. Verify the auto-created project exists:
   - Job name: `aurora-hms-ci-cd`
5. In GitHub repo settings, add webhook:
   - URL: `http://<jenkins-host>:18088/github-webhook/`
   - Content type: `application/json`
   - Event: `Just the push event`

## 3) Pipeline build steps (recommended)

Use these stages in your Jenkins pipeline:

1. **Checkout**
   - Pull latest source from repository.
2. **Backend Build**
   - Run: `cd easyops-erp && ./mvnw clean install -DskipTests`
3. **Backend Test**
   - Run: `cd easyops-erp && ./mvnw clean test`
4. **Frontend Install + Build**
   - Run:
     - `cd easyops-erp/frontend && npm ci`
     - `npm run lint`
     - `npm run type-check`
     - `npm run build`
5. **Docker Build (optional)**
   - Build service images if your deployment flow needs container artifacts.

## 4) Jenkinsfile included in repo

A production-ready Jenkins pipeline file is now added at:

`easyops-erp/Jenkinsfile`

It includes:

- Checkout
- Project layout validation (`pom.xml` and `frontend/package.json`)
- Backend build (`mvnw clean install -DskipTests`)
- Backend tests (`mvnw test`)
- Frontend CI checks (`npm ci`, lint, type-check, build)
- Docker image build on `main`/`master`
- Optional deploy stage (`Deploy Project`) on `main`/`master` when `DEPLOY_ENABLED=true`

## 5) Bootstrap environment variables

The compose file uses these variables for job bootstrap:

- `JENKINS_BOOTSTRAP_JOB_NAME` (default `aurora-hms-ci-cd`)
- `JENKINS_BOOTSTRAP_GITHUB_URL` (required for auto-create)
- `JENKINS_BOOTSTRAP_BRANCH` (default `*/main`)
- `JENKINS_BOOTSTRAP_SCRIPT_PATH` (default `easyops-erp/Jenkinsfile`)
- `JENKINS_BOOTSTRAP_CREDENTIALS_ID` (optional)

## 6) Enable CD stage (optional)

By default, deploy is guarded and runs only when `DEPLOY_ENABLED=true`.

To enable deployment:

1. Open Jenkins job -> **Configure** -> **Build Environment**.
2. Add environment variable:
   - Name: `DEPLOY_ENABLED`
   - Value: `true`
3. Ensure Jenkins node has Docker permission and access to deployment host/resources.

## 7) Legacy inline example (reference only)

```groovy
pipeline {
  agent any

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Backend Build') {
      steps {
        dir('easyops-erp') {
          sh './mvnw clean install -DskipTests'
        }
      }
    }

    stage('Backend Test') {
      steps {
        dir('easyops-erp') {
          sh './mvnw clean test'
        }
      }
    }

    stage('Frontend Build') {
      steps {
        dir('easyops-erp/frontend') {
          sh 'npm ci'
          sh 'npm run lint'
          sh 'npm run type-check'
          sh 'npm run build'
        }
      }
    }
  }
}
```

If your Jenkins controller runs on Windows agents, replace `sh` with `bat` in the Jenkinsfile.
