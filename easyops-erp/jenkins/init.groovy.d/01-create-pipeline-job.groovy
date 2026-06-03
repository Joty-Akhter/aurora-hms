import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

def jenkins = Jenkins.instance

def jobName = System.getenv('JENKINS_BOOTSTRAP_JOB_NAME') ?: 'aurora-hms-ci-cd'
def repoUrl = System.getenv('JENKINS_BOOTSTRAP_GITHUB_URL')
def branch = System.getenv('JENKINS_BOOTSTRAP_BRANCH') ?: '*/main'
def credentialsId = System.getenv('JENKINS_BOOTSTRAP_CREDENTIALS_ID')
def scriptPath = System.getenv('JENKINS_BOOTSTRAP_SCRIPT_PATH') ?: 'easyops-erp/Jenkinsfile'

if (!repoUrl || repoUrl.trim().isEmpty()) {
    println('[bootstrap] Skipping job creation. Set JENKINS_BOOTSTRAP_GITHUB_URL to enable.')
    return
}

def existing = jenkins.getItem(jobName)
if (existing != null) {
    println("[bootstrap] Job '${jobName}' already exists. Skipping.")
    return
}

def remoteConfig = new UserRemoteConfig(repoUrl, null, null, credentialsId ?: null)
def scm = new GitSCM(
        [remoteConfig],
        [new BranchSpec(branch)],
        false,
        [],
        null,
        null,
        []
)

def job = jenkins.createProject(WorkflowJob, jobName)
job.definition = new CpsScmFlowDefinition(scm, scriptPath)
job.description = 'Auto-bootstrapped Aurora HMS CI/CD pipeline.'
job.save()

println("[bootstrap] Created Jenkins pipeline job '${jobName}' from ${repoUrl}.")
