pipeline {
    agent {
        label 'java'
    }
    
    stages {
        stage('Fetch code') {
            steps {
                //checkout scm
                sh 'git clone https://github.com/indigo-dc/orchestrator'
            }
        }
        
        stage('Style Analysis') {
            steps {
                dir("$WORKSPACE/orchestrator") {
                    echo 'Running checkstyle..'
                    sh 'mvn -Dcheckstyle.failOnViolation=true -Dcheckstyle.console=true -Dcheckstyle.violationSeverity=warning -Dcheckstyle.config.location=google_checks.xml checkstyle:check'
                    echo 'Parsing checkstyle logs..'
                    checkstyle canComputeNew: false,
                               defaultEncoding: '',
                               healthy: '',
                               pattern: '**/target/checkstyle-result.xml',
                               unHealthy: ''
                } // dir: $WORKSPACE/orchestrator
                dir("$WORKSPACE/orchestrator/target") {
                    deleteDir()   
                }
            }
        } // code style stage
        
        stage('Unit tests') {
            steps {
                dir("$WORKSPACE/orchestrator") {
                    echo 'Running cobertura..'
                    sh 'mvn -Dcobertura.report.format=xml cobertura:cobertura'
                    echo 'Publishing Cobertura coverage report..'
                    cobertura autoUpdateHealth: false,
                              autoUpdateStability: false,
                              coberturaReportFile: '**/target/site/cobertura/coverage.xml',
                              conditionalCoverageTargets: '70, 0, 0',
                              failUnhealthy: false,
                              failUnstable: false,
                              lineCoverageTargets: '80, 0, 0',
                              maxNumberOfBuilds: 0,
                              methodCoverageTargets: '80, 0, 0',
                              onlyStable: false,
                              sourceEncoding: 'ASCII',
                              zoomCoverageChart: false
                    echo 'Publishing JUnit test result report'
                    junit '**/target/surefire-reports/*.xml'
                }
                dir("$WORKSPACE/orchestrator/target") {
                    deleteDir()   
                }
            }
        }
        
        stage('Integration tests') {
            steps {
                dir("$WORKSPACE/orchestrator") {
                    echo 'Running integration tests..'
                    sh 'mvn integration-test'
                    echo 'Publishing JUnit test result report'
                    junit '**/target/surefire-reports/*.xml'
                }
                dir("$WORKSPACE/orchestrator/target") {
                    deleteDir()   
                }
            }
        }
        
        stage('Metrics') {
            agent {
                label 'sloc'
            }
            steps {
                dir("$WORKSPACE/orchestrator") {
                    echo 'Getting SLOC..'
                    sh 'cloc --by-file --xml --out=cloc.xml .'
                    sloccountPublish encoding: '', pattern: '**/cloc.xml'
                }
            }    
        }
    } // stages
} // pipeline
