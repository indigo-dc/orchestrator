#!/usr/bin/groovy

@Library(['github.com/indigo-dc/jenkins-pipeline-library']) _

pipeline {
    agent {
        label 'java'
    }

    environment {
        dockerhub_repo = "indigodatacloud/orchestrator"
        dockerhub_image_id = ""
    }

    stages {
        stage('Fetch code') {
            steps {
                checkout scm
            }
        }

        stage('Style Analysis') {
            steps {
                MavenRun('checkstyle')
            }
            post {
                always {
                    CheckstyleReport()
                    dir("$WORKSPACE/target") {
                        deleteDir()
                    }
                }
            }
        }

        stage('Unit testing coverage') {
            steps {
                MavenRun('cobertura')
            }
            post {
                success {
                    CoberturaReport('**/target/site/cobertura/coverage.xml')
                    JUnitReport()
                    dir("$WORKSPACE/target") {
                        deleteDir()
                    }
                }
            }
        }

        stage('Integration tests') {
            steps {
                MavenRun('integration-test')
            }
            post {
                success {
                    JUnitReport()
                }
            }
        }

        stage('Dependency check') {
            agent {
                label 'docker-build'
            }
            steps {
                checkout scm
                OWASPDependencyCheckRun("$WORKSPACE/orchestrator/src", project="Orchestrator")
            }
            post {
                always {
                    OWASPDependencyCheckPublish()
                    HTMLReport('src', 'dependency-check-report.html', 'OWASP Dependency Report')
                    deleteDir()
                }
            }
        }

        stage('Metrics') {
            agent {
                label 'sloc'
            }
            steps {
                checkout scm
                SLOCRun()
            }
            post {
                success {
                    SLOCPublish()
                }
            }
        }

        stage('DockerHub delivery') {
            when {
                anyOf {
                    branch 'master'
                    branch 'releases/*'
                    tag 'v*'
                }
            }
            agent {
                label 'docker-build'
            }
            steps {
                checkout scm
                script {
                    PROJECT_VERSION="""${sh([
                        returnStdout: true,
                        script: 'mvn -q -Dexec.executable=echo -Dexec.args=\'${project.version}\' --non-recursive exec:exec']).trim()
                    }"""
                    MavenRun('-DskipTests=true package')
                    dockerhub_image_id = DockerBuild(
                        dockerhub_repo,
                        PROJECT_VERSION,
                        'docker')
                }
            }
            post {
                success {
                    DockerPush(dockerhub_image_id)
                }
                failure {
                    DockerClean()
                }
                always {
                    cleanWs()
                }
            }
        }

        stage('Notifications') {
            when {
                tag 'v*'
            }
            steps {
                JiraIssueNotification(
                    'DEEP',
                    'DPM',
                    '10204',
                    "[preview-testbed] New Orchestrator version ${env.BRANCH_NAME} available",
                    "Check new artifacts at:\n\t- Docker image: [${dockerhub_image_id}|https://hub.docker.com/r/${dockerhub_repo}/tags/]",
                    ['wp3', 'preview-testbed', "orchestrator-${env.BRANCH_NAME}"],
                    'Task',
                    'mariojmdavid'
                )
            }
        }
    } // stages
} // pipeline
